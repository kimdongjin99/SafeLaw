import React, { useRef, useState } from 'react';
import {
  ActivityIndicator,
  BackHandler,
  FlatList,
  KeyboardAvoidingView,
  NativeModules,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { ArrowLeft, Bot, Send, User } from 'lucide-react-native';

import { colors } from './src/theme/colors';
import { Message, saveChatMessage } from './src/services/chatStorage';

interface SafeLawNativeModule {
  ask(question: string): Promise<string>;
}

const llamaModule = NativeModules.SafeLawModule as
  | SafeLawNativeModule
  | undefined;

function App() {
  const [sessionId] = useState(() => Date.now().toString());
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const flatListRef = useRef<FlatList<Message>>(null);

  const handleSend = async () => {
    if (!input.trim() || isLoading) {
      return;
    }

    const userText = input.trim();
    const requestId = Date.now();
    setInput('');

    const userMsg: Message = {
      id: `${requestId}-user`,
      sender: 'user',
      text: userText,
      timestamp: new Date().toISOString(),
    };

    setMessages(previous => [...previous, userMsg]);

    try {
      await saveChatMessage(sessionId, userMsg);
    } catch (error) {
      console.error('사용자 메시지 저장 실패:', error);
    }

    setIsLoading(true);

    try {
      if (!llamaModule) {
        throw new Error('SafeLawModule이 네이티브 앱에 등록되지 않았습니다.');
      }

      const answer = await llamaModule.ask(userText);
      const aiMsg: Message = {
        id: `${requestId}-ai`,
        sender: 'ai',
        text: answer.trim() || '모델이 빈 답변을 반환했습니다.',
        timestamp: new Date().toISOString(),
      };

      setMessages(previous => [...previous, aiMsg]);

      try {
        await saveChatMessage(sessionId, aiMsg);
      } catch (error) {
        console.error('모델 답변 저장 실패:', error);
      }
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error ? error.message : String(error);
      const errorMsg: Message = {
        id: `${requestId}-error`,
        sender: 'ai',
        text: `모델 실행 오류: ${errorMessage}`,
        timestamp: new Date().toISOString(),
      };

      setMessages(previous => [...previous, errorMsg]);

      try {
        await saveChatMessage(sessionId, errorMsg);
      } catch (storageError) {
        console.error('오류 메시지 저장 실패:', storageError);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => BackHandler.exitApp()}
            style={styles.backBtn}
          >
            <ArrowLeft color={colors.foreground} size={24} />
          </TouchableOpacity>
          <View style={styles.headerTitleContainer}>
            <Text style={styles.headerTitle}>AI 법률 상담</Text>
            <Text style={styles.headerSubtitle}>
              온디바이스 보안 모드 작동 중
            </Text>
          </View>
        </View>

        <KeyboardAvoidingView
          style={styles.flex}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          keyboardVerticalOffset={Platform.OS === 'ios' ? 10 : 0}
        >
          <FlatList
            ref={flatListRef}
            data={messages}
            keyExtractor={item => item.id}
            contentContainerStyle={styles.messageList}
            keyboardShouldPersistTaps="handled"
            onContentSizeChange={() =>
              flatListRef.current?.scrollToEnd({ animated: true })
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
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
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
  messageList: { flexGrow: 1, padding: 16, gap: 16 },
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
  emptyContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
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

export default App;
