import React, {useRef, useState} from 'react';
import {
  FlatList,
  KeyboardAvoidingView,
  NativeModules,
  Platform,
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {Menu, Plus, Send, ShieldCheck} from 'lucide-react-native';
import {
  SafeAreaProvider,
  SafeAreaView,
} from 'react-native-safe-area-context';

type Message = {
  id: string;
  role: 'assistant' | 'user';
  text: string;
  time: string;
};

interface SafeLawNativeModule {
  ask(question: string): Promise<string>;
}

const llamaModule = NativeModules.LlamaModule as
  | SafeLawNativeModule
  | undefined;

const INITIAL_MESSAGES: Message[] = [
  {
    id: 'welcome',
    role: 'assistant',
    text: '안녕하세요. SafeLaw입니다.\n궁금한 법률 문제를 편하게 질문해 주세요.',
    time: '오후 8:46',
  },
];

function currentTime(): string {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date());
}

function App() {
  const [messages, setMessages] = useState(INITIAL_MESSAGES);
  const [draft, setDraft] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const listRef = useRef<FlatList<Message>>(null);

  const sendMessage = async () => {
    const text = draft.trim();
    if (!text || isGenerating) {
      return;
    }

    const requestId = Date.now();
    setMessages(current => [
      ...current,
      {
        id: `${requestId}-user`,
        role: 'user',
        text,
        time: currentTime(),
      },
    ]);
    setDraft('');
    setIsGenerating(true);

    try {
      if (!llamaModule) {
        throw new Error('LlamaModule이 네이티브 앱에 등록되지 않았습니다.');
      }

      const answer = await llamaModule.ask(text);
      setMessages(current => [
        ...current,
        {
          id: `${requestId}-assistant`,
          role: 'assistant',
          text: answer.trim() || '모델이 빈 답변을 반환했습니다.',
          time: currentTime(),
        },
      ]);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setMessages(current => [
        ...current,
        {
          id: `${requestId}-error`,
          role: 'assistant',
          text: `모델 실행 오류: ${message}`,
          time: currentTime(),
        },
      ]);
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
        <StatusBar barStyle="dark-content" backgroundColor="#B2C7D9" />
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          keyboardVerticalOffset={0}
          style={styles.flex}>
          <View style={styles.header}>
            <View style={styles.brandMark}>
              <ShieldCheck color="#FFFFFF" size={20} strokeWidth={2.2} />
            </View>
            <View style={styles.headerCopy}>
              <Text style={styles.title}>SafeLaw</Text>
              <Text style={styles.subtitle}>법률 상담 도우미</Text>
            </View>
            <Pressable
              accessibilityLabel="메뉴 열기"
              accessibilityRole="button"
              hitSlop={10}
              style={({pressed}) => [
                styles.headerButton,
                pressed && styles.pressed,
              ]}>
              <Menu color="#29343C" size={23} />
            </Pressable>
          </View>

          <FlatList
            ref={listRef}
            contentContainerStyle={styles.messageList}
            data={messages}
            keyExtractor={item => item.id}
            keyboardShouldPersistTaps="handled"
            onContentSizeChange={() => listRef.current?.scrollToEnd({animated: true})}
            renderItem={({item}) => {
              const isUser = item.role === 'user';

              return (
                <View
                  style={[
                    styles.messageRow,
                    isUser ? styles.userRow : styles.assistantRow,
                  ]}>
                  {!isUser && (
                    <View style={styles.avatar}>
                      <ShieldCheck color="#FFFFFF" size={17} />
                    </View>
                  )}
                  <View style={styles.messageBody}>
                    {!isUser && <Text style={styles.sender}>SafeLaw</Text>}
                    <View style={styles.bubbleLine}>
                      {isUser && <Text style={styles.time}>{item.time}</Text>}
                      <View
                        style={[
                          styles.bubble,
                          isUser ? styles.userBubble : styles.assistantBubble,
                        ]}>
                        <Text style={styles.messageText}>{item.text}</Text>
                      </View>
                      {!isUser && <Text style={styles.time}>{item.time}</Text>}
                    </View>
                  </View>
                </View>
              );
            }}
          />

          <View style={styles.composer}>
            <Pressable
              accessibilityLabel="첨부하기"
              accessibilityRole="button"
              hitSlop={8}
              style={({pressed}) => [
                styles.addButton,
                pressed && styles.pressed,
              ]}>
              <Plus color="#66737C" size={25} />
            </Pressable>
            <TextInput
              accessibilityLabel="메시지 입력"
              multiline
              onChangeText={setDraft}
              onSubmitEditing={sendMessage}
              placeholder="법률 질문을 입력하세요"
              placeholderTextColor="#8B959C"
              returnKeyType="send"
              style={styles.input}
              value={draft}
            />
            <Pressable
              accessibilityLabel="메시지 보내기"
              accessibilityRole="button"
              disabled={!draft.trim() || isGenerating}
              onPress={sendMessage}
              style={({pressed}) => [
                styles.sendButton,
                (!draft.trim() || isGenerating) && styles.sendButtonDisabled,
                pressed && styles.pressed,
              ]}>
              <Send color="#FFFFFF" size={18} strokeWidth={2.4} />
            </Pressable>
          </View>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  flex: {flex: 1},
  safeArea: {flex: 1, backgroundColor: '#B2C7D9'},
  header: {
    minHeight: 64,
    paddingHorizontal: 17,
    flexDirection: 'row',
    alignItems: 'center',
    borderBottomColor: 'rgba(61, 80, 94, 0.12)',
    borderBottomWidth: StyleSheet.hairlineWidth,
    backgroundColor: '#B2C7D9',
  },
  brandMark: {
    width: 38,
    height: 38,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#315C73',
  },
  headerCopy: {flex: 1, marginLeft: 11},
  title: {fontSize: 17, color: '#1E2930', fontWeight: '700'},
  subtitle: {marginTop: 2, fontSize: 11, color: '#596A75'},
  headerButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  messageList: {
    flexGrow: 1,
    paddingHorizontal: 14,
    paddingTop: 22,
    paddingBottom: 20,
    gap: 16,
  },
  messageRow: {flexDirection: 'row', alignItems: 'flex-start'},
  assistantRow: {paddingRight: 44},
  userRow: {justifyContent: 'flex-end', paddingLeft: 44},
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 13,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#315C73',
    marginRight: 8,
  },
  messageBody: {maxWidth: '88%'},
  sender: {fontSize: 12, color: '#34434C', marginBottom: 5, marginLeft: 2},
  bubbleLine: {flexDirection: 'row', alignItems: 'flex-end', gap: 5},
  bubble: {
    borderRadius: 16,
    paddingHorizontal: 13,
    paddingVertical: 10,
    shadowColor: '#3C4D58',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.08,
    shadowRadius: 2,
    elevation: 1,
  },
  assistantBubble: {backgroundColor: '#FFFFFF', borderTopLeftRadius: 4},
  userBubble: {backgroundColor: '#FEE500', borderTopRightRadius: 4},
  messageText: {fontSize: 15, lineHeight: 21, color: '#20272C'},
  time: {fontSize: 10, color: '#65747D', marginBottom: 2},
  composer: {
    minHeight: 66,
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 9,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderTopColor: '#E2E5E7',
    borderTopWidth: StyleSheet.hairlineWidth,
    backgroundColor: '#FFFFFF',
  },
  addButton: {
    width: 38,
    height: 42,
    alignItems: 'center',
    justifyContent: 'center',
  },
  input: {
    flex: 1,
    maxHeight: 108,
    minHeight: 42,
    paddingHorizontal: 14,
    paddingTop: Platform.OS === 'ios' ? 11 : 8,
    paddingBottom: Platform.OS === 'ios' ? 10 : 8,
    borderColor: '#DDE2E5',
    borderWidth: 1,
    borderRadius: 21,
    backgroundColor: '#F7F8F9',
    fontSize: 15,
    lineHeight: 20,
    color: '#20272C',
  },
  sendButton: {
    width: 42,
    height: 42,
    borderRadius: 21,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#315C73',
  },
  sendButtonDisabled: {backgroundColor: '#BCC5CA'},
  pressed: {opacity: 0.72},
});

export default App;
