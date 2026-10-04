import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { MessageSquare, Trash2, ChevronRight, Plus } from 'lucide-react-native';
import { colors } from '../theme/colors';
import {
  ChatSession,
  getChatSessions,
  deleteChatSession,
} from '../services/chatStorage';

interface Props {
  navigation: any;
}

export function HistoryScreen({ navigation }: Props) {
  const [sessions, setSessions] = useState<ChatSession[]>([]);

  useFocusEffect(
    useCallback(() => {
      loadHistory();
    }, [])
  );

  const loadHistory = async () => {
    const data = await getChatSessions();
    setSessions(data);
  };

  const handleDelete = (id: string) => {
    Alert.alert('상담 내역 삭제', '이 상담 내역을 정말 삭제하시겠습니까?', [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: async () => {
          await deleteChatSession(id);
          loadHistory();
        },
      },
    ]);
  };

  const formatDate = (isoString: string) => {
    const date = new Date(isoString);
    return `${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()}`;
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>상담 히스토리</Text>
          <Text style={styles.subtitle}>기기에 안전하게 저장된 이전 상담 내역</Text>
        </View>
        <TouchableOpacity
          style={styles.newChatBtn}
          onPress={() => navigation.navigate('Chat')}
        >
          <Plus color={colors.white} size={20} />
        </TouchableOpacity>
      </View>

      <FlatList
        data={sessions}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => navigation.navigate('Chat', { sessionId: item.id })}
            activeOpacity={0.7}
          >
            <View style={styles.cardHeader}>
              <View style={styles.iconBox}>
                <MessageSquare color={colors.primary} size={20} />
              </View>
              <Text style={styles.cardTitle} numberOfLines={1}>
                {item.title}
              </Text>
              <TouchableOpacity
                onPress={() => handleDelete(item.id)}
                hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
              >
                <Trash2 color={colors.mutedForeground} size={18} />
              </TouchableOpacity>
            </View>

            <Text style={styles.lastMessage} numberOfLines={2}>
              {item.lastMessage}
            </Text>

            <View style={styles.cardFooter}>
              <Text style={styles.dateText}>{formatDate(item.updatedAt)}</Text>
              <ChevronRight color={colors.mutedForeground} size={16} />
            </View>
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <MessageSquare color={colors.mutedForeground} size={48} />
            <Text style={styles.emptyTitle}>저장된 상담 내역이 없습니다</Text>
            <Text style={styles.emptySubtitle}>
              새로운 질문을 시작하면 이곳에 기록이 저장됩니다.
            </Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingTop: 24,
    paddingBottom: 16,
  },
  title: { fontSize: 24, fontWeight: '700', color: colors.foreground, marginBottom: 4 },
  subtitle: { fontSize: 13, color: colors.mutedForeground },
  newChatBtn: {
    width: 40, height: 40, borderRadius: 20,
    backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center',
  },
  listContent: { paddingHorizontal: 24, paddingBottom: 24, gap: 12 },
  card: {
    padding: 16, borderRadius: 12, backgroundColor: colors.white,
    borderWidth: 1, borderColor: colors.border,
  },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 8 },
  iconBox: {
    width: 32, height: 32, borderRadius: 8,
    backgroundColor: colors.secondary, alignItems: 'center', justifyContent: 'center',
  },
  cardTitle: { flex: 1, fontSize: 15, fontWeight: '600', color: colors.foreground },
  lastMessage: { fontSize: 13, color: colors.mutedForeground, lineHeight: 18, marginBottom: 12 },
  cardFooter: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  dateText: { fontSize: 11, color: colors.mutedForeground },
  emptyContainer: { alignItems: 'center', justifyContent: 'center', marginTop: 120, paddingHorizontal: 32 },
  emptyTitle: { fontSize: 16, fontWeight: '600', color: colors.foreground, marginTop: 16, marginBottom: 6 },
  emptySubtitle: { fontSize: 13, color: colors.mutedForeground, textAlign: 'center' },
});