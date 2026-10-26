import AsyncStorage from '@react-native-async-storage/async-storage';

const CHAT_STORAGE_KEY = '@safelaw_chat_sessions';

export interface Message {
  id: string;
  sender: 'user' | 'ai';
  text: string;
  timestamp: string;
}

export interface ChatSession {
  id: string;
  title: string;
  lastMessage: string;
  updatedAt: string;
  messages: Message[];
}

// 1. 모든 상담 내역 목록 가져오기
export const getChatSessions = async (): Promise<ChatSession[]> => {
  try {
    const jsonValue = await AsyncStorage.getItem(CHAT_STORAGE_KEY);
    return jsonValue != null ? JSON.parse(jsonValue) : [];
  } catch (e) {
    console.error('채팅 히스토리 불러오기 실패:', e);
    return [];
  }
};

// 2. 특정 상담 상세 정보 가져오기
export const getChatSessionById = async (sessionId: string): Promise<ChatSession | null> => {
  try {
    const sessions = await getChatSessions();
    return sessions.find((s) => s.id === sessionId) || null;
  } catch (e) {
    console.error('채팅 세션 불러오기 실패:', e);
    return null;
  }
};

// 3. 메시지 저장 (신규 세션 자동 생성 또는 기존 세션 업데이트)
export const saveChatMessage = async (
  sessionId: string,
  message: Message,
  defaultTitle?: string
): Promise<ChatSession> => {
  try {
    const sessions = await getChatSessions();
    const existingIndex = sessions.findIndex((s) => s.id === sessionId);

    let targetSession: ChatSession;

    if (existingIndex > -1) {
      targetSession = sessions[existingIndex];
      targetSession.messages.push(message);
      targetSession.lastMessage = message.text;
      targetSession.updatedAt = message.timestamp;
      sessions[existingIndex] = targetSession;
    } else {
      targetSession = {
        id: sessionId,
        title: defaultTitle || (message.text.length > 20 ? message.text.slice(0, 20) + '...' : message.text),
        lastMessage: message.text,
        updatedAt: message.timestamp,
        messages: [message],
      };
      sessions.unshift(targetSession);
    }

    await AsyncStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(sessions));
    return targetSession;
  } catch (e) {
    console.error('메시지 저장 실패:', e);
    throw e;
  }
};

// 4. 특정 상담 내역 삭제
export const deleteChatSession = async (sessionId: string): Promise<void> => {
  try {
    const sessions = await getChatSessions();
    const filtered = sessions.filter((s) => s.id !== sessionId);
    await AsyncStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(filtered));
  } catch (e) {
    console.error('채팅 세션 삭제 실패:', e);
  }
};

// 5. 전체 상담 내역 삭제
export const clearAllChatSessions = async (): Promise<void> => {
  try {
    await AsyncStorage.removeItem(CHAT_STORAGE_KEY);
  } catch (e) {
    console.error('전체 채팅 삭제 실패:', e);
  }
};