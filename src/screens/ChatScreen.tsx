import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  KeyboardAvoidingView,
  NativeModules,
  NativeEventEmitter,
  NativeModule,
  Platform,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ArrowLeft, Send, Bot, User } from 'lucide-react-native';
import { colors } from '../theme/colors';
import {
  Message,
  getChatSessionById,
  saveChatMessage,
} from '../services/chatStorage';

interface ChatTurnPayload {
  question: string;
  answer: string;
}

interface SafeLawNativeModule extends NativeModule {
  ask(question: string): Promise<string>;
  askWithHistory?(question: string, history: ChatTurnPayload[]): Promise<string>;
}

// 이어지는 질문의 맥락용으로 AI에 함께 넘길 최근 대화 수와 길이
const MAX_HISTORY_TURNS = 3;
const MAX_HISTORY_ANSWER_CHARS = 200;

// 화면의 메시지 목록에서 (질문, 답변) 쌍을 만들어 최근 대화만 남긴다. 오류 메시지는 뺀다.
function buildRecentTurns(messages: Message[]): ChatTurnPayload[] {
  const turns: ChatTurnPayload[] = [];
  let pendingQuestion: string | null = null;
  for (const m of messages) {
    if (m.sender === 'user') {
      pendingQuestion = m.text;
    } else if (pendingQuestion) {
      const isError = m.text.startsWith('오류가 발생') || m.text.startsWith('모델 실행 오류');
      if (!isError) {
        turns.push({ question: pendingQuestion, answer: m.text.slice(0, MAX_HISTORY_ANSWER_CHARS) });
      }
      pendingQuestion = null;
    }
  }
  return turns.slice(-MAX_HISTORY_TURNS);
}

const safeLawModule = NativeModules.SafeLawModule as
  | SafeLawNativeModule
  | undefined;

type GenerationStage =
  | 'EMBEDDING_QUESTION'
  | 'SEARCHING_PRECEDENTS'
  | 'GENERATING_ANSWER'
  | 'POST_PROCESSING'
  | 'COMPLETED'
  | 'FAILED';

const STAGE_LABELS: Record<GenerationStage, string> = {
  EMBEDDING_QUESTION: '질문을 분석하고 있습니다...',
  SEARCHING_PRECEDENTS: '관련 판례를 검색하고 있습니다...',
  GENERATING_ANSWER: '답변을 생성하고 있습니다...',
  POST_PROCESSING: '답변을 정리하고 있습니다...',
  COMPLETED: '답변 생성이 완료되었습니다.',
  FAILED: '답변 생성 중 문제가 발생했습니다.',
};

interface Props {
  navigation: any;
  route: any;
}

// 서버 응답 타입
interface LawTextItem {
  id: number;
  caseName: string;
  caseNum: string;
  courtName: string;
  content: string;
}

export function ChatScreen({ navigation, route }: Props) {
  const existingSessionId = route.params?.sessionId;
  const [sessionId] = useState<string>(
    existingSessionId || Date.now().toString()
  );
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [generationStage, setGenerationStage] =
    useState<GenerationStage | null>(null);
  const [streamingText, setStreamingText] = useState('');

  const flatListRef = useRef<FlatList>(null);
  const isGeneratingRef = useRef(false);
  const streamingTextRef = useRef('');

  useEffect(() => {
    if (existingSessionId) {
      const loadSession = async () => {
        const session = await getChatSessionById(existingSessionId);
        if (session) {
          setMessages(session.messages);
        }
      };
      loadSession();
    }
  }, [existingSessionId]);

  // SafeLawModule 이벤트 리스너 등록 (진행 단계 + 실시간 토큰 스트리밍)
  useEffect(() => {
    if (!safeLawModule) return;

    const eventEmitter = new NativeEventEmitter(safeLawModule);

    const stageSubscription = eventEmitter.addListener(
      'SafeLawGenerationStage',
      (payload: unknown) => {
        if (!isGeneratingRef.current) return;

        const nextStage = (
          typeof payload === 'string'
            ? payload
            : (payload as { stage?: string; status?: string })?.stage ??
              (payload as { stage?: string; status?: string })?.status
        ) as GenerationStage | undefined;

        if (nextStage) {
          setGenerationStage(nextStage);
        }
      }
    );

    const tokenSubscription = eventEmitter.addListener(
      'SafeLawToken',
      (payload: unknown) => {
        if (!isGeneratingRef.current) return;

        const token =
          typeof payload === 'string'
            ? payload
            : (payload as { token?: string; text?: string })?.token ??
              (payload as { token?: string; text?: string })?.text ??
              '';

        if (token) {
          streamingTextRef.current += token;
          setStreamingText(streamingTextRef.current);
        }
      }
    );

    return () => {
      stageSubscription.remove();
      tokenSubscription.remove();
    };
  }, []);

  // (가상) 온디바이스 벡터 검색 함수
  // 실제 프로젝트의 벡터 검색 로직으로 대체해야 합니다.
  const searchOnDeviceVectorDb = async (query: string): Promise<number[]> => {
    // 예시: 기기 내에서 검색한 진짜 판례 ID 배열 순위 반환
    return [107, 122165];
  };

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;

    const userText = input.trim();
    setInput('');

    const userMsg: Message = {
      id: Date.now().toString(),
      sender: 'user',
      text: userText,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    await saveChatMessage(sessionId, userMsg);

    // 스트리밍 및 진행 상태 초기화
    isGeneratingRef.current = true;
    streamingTextRef.current = '';
    setStreamingText('');
    setGenerationStage('EMBEDDING_QUESTION');
    setIsLoading(true);

    try {
      if (!safeLawModule) {
        throw new Error('SafeLawModule이 네이티브 앱에 등록되지 않았습니다.');
      }

      /* origin/main 판례 원문 직접 조회 구현
      // 1. 기기 안에서 벡터 검색으로 판례 번호와 유사도 순위 획득
      const realRankedIds = await searchOnDeviceVectorDb(userText);

      let aiText = '';

      if (realRankedIds.length === 0) {
        aiText = '관련된 판례를 찾을 수 없습니다.';
      } else {
        // 2. 가짜 번호를 섞어 총 1~30개 범위로 생성
        const dummyIds = [90001, 999999, 12345, 67890]; 
        const mixedIds = Array.from(new Set([...realRankedIds, ...dummyIds])).slice(0, 30);

        // 3. 판례 본문 조회 API 호출 (검색 x, 본문만 요청)
        const response = await apiClient.post<LawTextItem[]>('/api/v1/laws/texts', {
          ids: mixedIds,
        });

        // 4. 응답에서 가짜 번호 판례를 버리고 기존 순위대로 재정렬
        const textMap = new Map<number, LawTextItem>(
          response.data.map((item) => [item.id, item])
        );

        const finalResults = realRankedIds
          .map((id) => textMap.get(id))
          .filter((item): item is LawTextItem => item !== undefined);

        // 응답 텍스트 포매팅 (새로운 응답 필드 적용)
        aiText = finalResults
          .map(
            (item, idx) =>
              `[참고 판례 ${idx + 1}] ${item.caseName || '제목 없음'} (${item.caseNum})\n재판소: ${item.courtName}\n\n${item.content}`
          )
          .join('\n\n---\n\n');
      }
      */

      // 같은 대화방의 최근 대화를 함께 넘겨 이어지는 질문을 이해하게 한다
      const recentTurns = buildRecentTurns(messages);
      const answer =
        safeLawModule.askWithHistory && recentTurns.length > 0
          ? await safeLawModule.askWithHistory(userText, recentTurns)
          : await safeLawModule.ask(userText);
      const finalText =
        answer?.trim() ||
        streamingTextRef.current.trim() ||
        '모델이 빈 답변을 반환했습니다.';

      const aiMsg: Message = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        text: finalText,
        // origin/main: text: aiText || '관련 판례 정보를 성공적으로 조회했습니다.',
        timestamp: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, aiMsg]);
      await saveChatMessage(sessionId, aiMsg);
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error ? error.message : String(error);
      const errorMsg: Message = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        text: `모델 실행 오류: ${errorMessage}`,
        timestamp: new Date().toISOString(),
      };

      /* origin/main HTTP API 오류 처리 구현
    } catch (error: any) {
      console.error('Chat API Error Status:', error.response?.status);
      
      // 분기를 message가 아닌 code로 처리
      const errorCode = error.response?.data?.code || 'UNKNOWN_ERROR';
      const errorMessage = error.response?.data?.message || '요청 형식이 잘못되었거나 서버 오류가 발생했습니다.';

      const errorMsg: Message = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        text: `오류가 발생했습니다 [${errorCode}]: ${errorMessage}`,
        timestamp: new Date().toISOString(),
      };
      */

      setMessages((prev) => [...prev, errorMsg]);
      await saveChatMessage(sessionId, errorMsg);
    } finally {
      isGeneratingRef.current = false;
      streamingTextRef.current = '';
      setStreamingText('');
      setGenerationStage(null);
      setIsLoading(false);
    }
  };

  // 생성 중일 때 FlatList 맨 아래에 렌더링되는 실시간 스트리밍 말풍선
  const renderStreamingFooter = () => {
    if (!isLoading) return null;

    const stageLabel = generationStage
      ? STAGE_LABELS[generationStage]
      : '답변을 준비하고 있습니다...';
    const showStageIndicator =
      generationStage !== 'COMPLETED' && generationStage !== 'FAILED';

    return (
      <View style={[styles.messageBubbleContainer, styles.aiRow]}>
        <View style={styles.aiAvatar}>
          <Bot color={colors.white} size={16} />
        </View>
        <View style={[styles.bubble, styles.aiBubble]}>
          {streamingText.length > 0 && (
            <Text style={styles.aiText}>{streamingText}</Text>
          )}
          {showStageIndicator && (
            <View
              style={[
                styles.stageContainer,
                streamingText.length > 0 && styles.stageDivider,
              ]}
            >
              <ActivityIndicator size="small" color={colors.primary} />
              <Text style={styles.stageText}>{stageLabel}</Text>
            </View>
          )}
        </View>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          style={styles.backBtn}
        >
          <ArrowLeft color={colors.foreground} size={24} />
        </TouchableOpacity>
        <View style={styles.headerTitleContainer}>
          <Text style={styles.headerTitle}>AI 법률 상담</Text>
          <Text style={styles.headerSubtitle}>온디바이스 보안 모드 작동 중</Text>
        </View>
      </View>

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 10 : 0}
      >
        <FlatList
          ref={flatListRef}
          data={messages}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.messageList}
          onContentSizeChange={() =>
            flatListRef.current?.scrollToEnd({ animated: !isLoading })
          }
          renderItem={({ item }) => {
            const isUser = item.sender === 'user';
            return (
              <View
                style={[
                  styles.messageBubbleContainer,
                  isUser ? styles.userRow : styles.aiRow,
                ]}
              >
                {!isUser && (
                  <View style={styles.aiAvatar}>
                    <Bot color={colors.white} size={16} />
                  </View>
                )}
                <View
                  style={[
                    styles.bubble,
                    isUser ? styles.userBubble : styles.aiBubble,
                  ]}
                >
                  <Text style={isUser ? styles.userText : styles.aiText}>
                    {item.text}
                  </Text>
                </View>
                {isUser && (
                  <View style={styles.userAvatar}>
                    <User color={colors.white} size={16} />
                  </View>
                )}
              </View>
            );
          }}
          ListFooterComponent={renderStreamingFooter}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Bot color={colors.primary} size={48} />
              <Text style={styles.emptyTitle}>무엇이든 물어보세요</Text>
              <Text style={styles.emptySubtitle}>
                임대차 계약, 교통사고, 손해배상 등 법률 고민을 입력해주시면
                AI가 분석해 드립니다.
              </Text>
            </View>
          }
        />

        <View style={styles.inputContainer}>
          <TextInput
            style={styles.input}
            value={input}
            onChangeText={setInput}
            placeholder="법률 질문을 입력하세요..."
            placeholderTextColor={colors.mutedForeground}
            multiline
            editable={!isLoading}
          />
          <TouchableOpacity
            style={[
              styles.sendBtn,
              (!input.trim() || isLoading) && styles.disabledSendBtn,
            ]}
            onPress={handleSend}
            disabled={!input.trim() || isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color={colors.white} size="small" />
            ) : (
              <Send color={colors.white} size={18} />
            )}
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  backBtn: { padding: 8, marginRight: 8 },
  headerTitleContainer: { flex: 1 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: colors.foreground },
  headerSubtitle: { fontSize: 12, color: colors.mutedForeground },
  messageList: { padding: 16, gap: 16 },
  messageBubbleContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 8,
    marginBottom: 12,
  },
  userRow: { justifyContent: 'flex-end' },
  aiRow: { justifyContent: 'flex-start' },
  aiAvatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  userAvatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: colors.mutedForeground,
    alignItems: 'center',
    justifyContent: 'center',
  },
  bubble: { maxWidth: '75%', padding: 12, borderRadius: 16 },
  userBubble: { backgroundColor: colors.primary, borderBottomRightRadius: 2 },
  aiBubble: { backgroundColor: colors.secondary, borderBottomLeftRadius: 2 },
  userText: { color: colors.white, fontSize: 14, lineHeight: 20 },
  aiText: { color: colors.foreground, fontSize: 14, lineHeight: 20 },
  stageContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  stageDivider: {
    marginTop: 8,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  stageText: {
    fontSize: 12,
    color: colors.mutedForeground,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 100,
    paddingHorizontal: 32,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: colors.foreground,
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 13,
    color: colors.mutedForeground,
    textAlign: 'center',
    lineHeight: 18,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    gap: 8,
    backgroundColor: colors.white,
  },
  input: {
    flex: 1,
    maxHeight: 100,
    backgroundColor: colors.inputBackground,
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 14,
    color: colors.foreground,
  },
  sendBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  disabledSendBtn: { opacity: 0.5 },
});