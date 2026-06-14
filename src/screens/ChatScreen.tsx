import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Send, Sparkles } from 'lucide-react-native';
import { colors } from '../theme/colors';

interface Message {
  id: string;
  type: 'user' | 'assistant';
  content: string;
}

const QUICK_ACTIONS = ['계약서 검토', '임대차 문제', '노동 문제'];

export function ChatScreen() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      type: 'assistant',
      content: '안녕하세요! 법률 AI 어시스턴트입니다. 어떤 법률 문제로 도움이 필요하신가요?',
    },
  ]);
  const [inputValue, setInputValue] = useState('');
  const flatListRef = useRef<FlatList>(null);

  const handleSend = () => {
    if (!inputValue.trim()) return;

    const newMsg: Message = {
      id: Date.now().toString(),
      type: 'user',
      content: inputValue,
    };
    setMessages((prev) => [...prev, newMsg]);
    setInputValue('');

    setTimeout(() => {
      setMessages((prev) => [
        ...prev,
        {
          id: (Date.now() + 1).toString(),
          type: 'assistant',
          content: '해당 질문에 대해 법률 검토 중입니다. 잠시만 기다려주세요.',
        },
      ]);
    }, 1000);
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.avatarBox}>
          <Sparkles color={colors.primary} size={20} />
        </View>
        <View>
          <Text style={styles.headerTitle}>AI 법률 어시스턴트</Text>
          <Text style={styles.headerSub}>온디바이스 처리 중</Text>
        </View>
      </View>

      {/* Messages */}
      <FlatList
        ref={flatListRef}
        data={messages}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.messageList}
        onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
        renderItem={({ item }) => <MessageBubble message={item} />}
      />

      {/* Quick Actions */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={styles.quickScroll}
        contentContainerStyle={styles.quickContent}
      >
        {QUICK_ACTIONS.map((label) => (
          <TouchableOpacity
            key={label}
            style={styles.quickBtn}
            onPress={() => setInputValue(label)}
          >
            <Text style={styles.quickBtnText}>{label}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {/* Input */}
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <View style={styles.inputArea}>
          <View style={styles.inputRow}>
            <TextInput
              style={styles.input}
              value={inputValue}
              onChangeText={setInputValue}
              placeholder="법률 질문을 입력하세요..."
              placeholderTextColor={colors.mutedForeground}
              onSubmitEditing={handleSend}
              returnKeyType="send"
            />
            <TouchableOpacity
              style={[styles.sendBtn, !inputValue.trim() && styles.sendBtnDisabled]}
              onPress={handleSend}
              disabled={!inputValue.trim()}
            >
              <Send color={colors.white} size={16} />
            </TouchableOpacity>
          </View>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function MessageBubble({ message }: { message: Message }) {
  const isUser = message.type === 'user';
  return (
    <View style={[styles.bubbleWrapper, isUser ? styles.bubbleRight : styles.bubbleLeft]}>
      <View style={[styles.bubble, isUser ? styles.bubbleUser : styles.bubbleAI]}>
        <Text style={[styles.bubbleText, isUser && { color: colors.white }]}>
          {message.content}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  header: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    paddingHorizontal: 16, paddingVertical: 14,
    borderBottomWidth: 1, borderBottomColor: colors.border,
  },
  avatarBox: {
    width: 40, height: 40, borderRadius: 20,
    backgroundColor: colors.primaryLight,
    alignItems: 'center', justifyContent: 'center',
  },
  headerTitle: { fontSize: 16, fontWeight: '600', color: colors.foreground },
  headerSub: { fontSize: 12, color: colors.mutedForeground },
  messageList: { padding: 16, gap: 12 },
  bubbleWrapper: { flexDirection: 'row' },
  bubbleLeft: { justifyContent: 'flex-start' },
  bubbleRight: { justifyContent: 'flex-end' },
  bubble: {
    maxWidth: '80%', paddingHorizontal: 16,
    paddingVertical: 12, borderRadius: 16,
  },
  bubbleUser: { backgroundColor: colors.primary, borderBottomRightRadius: 4 },
  bubbleAI: { backgroundColor: colors.secondary, borderBottomLeftRadius: 4 },
  bubbleText: { fontSize: 14, color: colors.foreground, lineHeight: 20 },
  quickScroll: { paddingVertical: 8 },
  quickContent: { gap: 8, paddingHorizontal: 16 },
  quickBtn: {
    paddingHorizontal: 16, paddingVertical: 8,
    backgroundColor: colors.secondary, borderRadius: 20,
  },
  quickBtnText: { fontSize: 13, color: colors.foreground },
  inputArea: {
    paddingHorizontal: 16, paddingVertical: 12,
    borderTopWidth: 1, borderTopColor: colors.border,
  },
  inputRow: {
    flexDirection: 'row', alignItems: 'center', gap: 8,
    backgroundColor: colors.inputBackground, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 8,
  },
  input: { flex: 1, fontSize: 14, color: colors.foreground },
  sendBtn: {
    width: 32, height: 32, backgroundColor: colors.primary,
    borderRadius: 8, alignItems: 'center', justifyContent: 'center',
  },
  sendBtnDisabled: { opacity: 0.5 },
});