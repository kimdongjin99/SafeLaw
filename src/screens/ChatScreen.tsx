import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  KeyboardAvoidingView,
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
import apiClient from '../api/client';

interface Props {
  navigation: any;
  route: any;
}

// 서버 응답 타입[cite: 14]
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
  const flatListRef = useRef<FlatList>(null);

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

    setIsLoading(true);

    try {
      // 1. 기기 안에서 벡터 검색으로 판례 번호와 유사도 순위 획득[cite: 14]
      const realRankedIds = await searchOnDeviceVectorDb(userText);

      let aiText = '';

      if (realRankedIds.length === 0) {
        aiText = '관련된 판례를 찾을 수 없습니다.';
      } else {
        // 2. 가짜 번호를 섞어 총 1~30개 범위로 생성[cite: 14]
        const dummyIds = [90001, 999999, 12345, 67890]; 
        const mixedIds = Array.from(new Set([...realRankedIds, ...dummyIds])).slice(0, 30);

        // 3. 판례 본문 조회 API 호출 (검색 x, 본문만 요청)[cite: 14, 16]
        const response = await apiClient.post<LawTextItem[]>('/api/v1/laws/texts', {
          ids: mixedIds,
        });

        // 4. 응답에서 가짜 번호 판례를 버리고 기존 순위대로 재정렬[cite: 14]
        const textMap = new Map<number, LawTextItem>(
          response.data.map((item) => [item.id, item])
        );

        const finalResults = realRankedIds
          .map((id) => textMap.get(id))
          .filter((item): item is LawTextItem => item !== undefined);

        // 응답 텍스트 포매팅 (새로운 응답 필드 적용)[cite: 14]
        aiText = finalResults
          .map(
            (item, idx) =>
              `[참고 판례 ${idx + 1}] ${item.caseName || '제목 없음'} (${item.caseNum})\n재판소: ${item.courtName}\n\n${item.content}`
          )
          .join('\n\n---\n\n');
      }

      const aiMsg: Message = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        text: aiText || '관련 판례 정보를 성공적으로 조회했습니다.',
        timestamp: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, aiMsg]);
      await saveChatMessage(sessionId, aiMsg);
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
      setMessages((prev) => [...prev, errorMsg]);
      await saveChatMessage(sessionId, errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
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
          onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
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
                <View style={[styles.bubble, isUser ? styles.userBubble : styles.aiBubble]}>
                  <Text style={isUser ? styles.userText : styles.aiText}>{item.text}</Text>
                </View>
                {isUser && (
                  <View style={styles.userAvatar}>
                    <User color={colors.white} size={16} />
                  </View>
                )}
              </View>
            );
          }}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Bot color={colors.primary} size={48} />
              <Text style={styles.emptyTitle}>무엇이든 물어보세요</Text>
              <Text style={styles.emptySubtitle}>
                임대차 계약, 교통사고, 손해배상 등 법률 고민을 입력해주시면 AI가 분석해 드립니다.
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
            style={[styles.sendBtn, (!input.trim() || isLoading) && styles.disabledSendBtn]}
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
  messageBubbleContainer: { flexDirection: 'row', alignItems: 'flex-end', gap: 8, marginBottom: 12 },
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
  emptyContainer: { alignItems: 'center', justifyContent: 'center', marginTop: 100, paddingHorizontal: 32 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: colors.foreground, marginTop: 16, marginBottom: 8 },
  emptySubtitle: { fontSize: 13, color: colors.mutedForeground, textAlign: 'center', lineHeight: 18 },
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