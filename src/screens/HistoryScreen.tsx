import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Clock, ChevronRight } from 'lucide-react-native';
import { colors } from '../theme/colors';

interface HistoryItem {
  id: string;
  title: string;
  preview: string;
  date: string;
  category: string;
}

interface Props {
  navigation: any;
}

const FILTERS = ['전체', '최근 7일', '최근 30일', '즐겨찾기'];

const HISTORY: HistoryItem[] = [
  {
    id: '1',
    title: '임대차 계약서 검토 요청',
    preview: '전세 계약서 상의 특약 사항에 대해 문의드립니다...',
    date: '2시간 전',
    category: '부동산/임대차',
  },
  {
    id: '2',
    title: '퇴직금 계산 방법',
    preview: '5년 근무 후 퇴직 시 퇴직금 계산 방법에 대해...',
    date: '어제',
    category: '노동/근로',
  },
  {
    id: '3',
    title: '온라인 쇼핑몰 환불 거부',
    preview: '구매한 제품의 하자로 인한 환불 요청이 거부...',
    date: '3일 전',
    category: '소비자보호',
  },
  {
    id: '4',
    title: '차용증 작성 방법',
    preview: '지인에게 돈을 빌려주는데 차용증을 작성하려고...',
    date: '1주일 전',
    category: '계약/문서',
  },
];

export function HistoryScreen({ navigation }: Props) {
  const [activeFilter, setActiveFilter] = useState('전체');

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>상담 히스토리</Text>
        <Text style={styles.subtitle}>이전 법률 상담 내역을 확인하세요</Text>
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={styles.filterScroll}
        contentContainerStyle={styles.filterContent}
      >
        {FILTERS.map((f) => (
          <TouchableOpacity
            key={f}
            style={[styles.filterBtn, activeFilter === f && styles.filterBtnActive]}
            onPress={() => setActiveFilter(f)}
          >
            <Text style={[styles.filterText, activeFilter === f && styles.filterTextActive]}>
              {f}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      <FlatList
        data={HISTORY}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => navigation.navigate('Chat')}
          >
            <View style={styles.cardIcon}>
              <Clock color={colors.mutedForeground} size={20} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={styles.cardTitle} numberOfLines={1}>{item.title}</Text>
              <Text style={styles.cardPreview} numberOfLines={2}>{item.preview}</Text>
              <View style={styles.cardMeta}>
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>{item.category}</Text>
                </View>
                <Text style={styles.dateText}>{item.date}</Text>
              </View>
            </View>
            <ChevronRight color={colors.mutedForeground} size={20} />
          </TouchableOpacity>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  header: { paddingHorizontal: 24, paddingTop: 32, paddingBottom: 16 },
  title: { fontSize: 26, fontWeight: '700', color: colors.foreground, marginBottom: 6 },
  subtitle: { fontSize: 14, color: colors.mutedForeground },
  filterScroll: { marginBottom: 12 },
  filterContent: { gap: 8, paddingHorizontal: 24 },
  filterBtn: {
    paddingHorizontal: 16, paddingVertical: 8,
    backgroundColor: colors.secondary, borderRadius: 20,
  },
  filterBtnActive: { backgroundColor: colors.primary },
  filterText: { fontSize: 13, color: colors.foreground },
  filterTextActive: { color: colors.white },
  list: { paddingHorizontal: 24, paddingBottom: 24, gap: 12 },
  card: {
    flexDirection: 'row', alignItems: 'flex-start', gap: 12,
    padding: 16, backgroundColor: colors.white,
    borderWidth: 1, borderColor: colors.border, borderRadius: 12,
  },
  cardIcon: {
    width: 40, height: 40, backgroundColor: colors.secondary,
    borderRadius: 8, alignItems: 'center', justifyContent: 'center',
  },
  cardTitle: { fontSize: 14, fontWeight: '600', color: colors.foreground, marginBottom: 4 },
  cardPreview: { fontSize: 12, color: colors.mutedForeground, marginBottom: 8 },
  cardMeta: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  badge: {
    paddingHorizontal: 8, paddingVertical: 3,
    backgroundColor: colors.primaryLight, borderRadius: 6,
  },
  badgeText: { fontSize: 11, color: colors.primary },
  dateText: { fontSize: 12, color: colors.mutedForeground },
});