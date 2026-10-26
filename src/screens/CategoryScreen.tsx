import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  FileText, Home, Briefcase, Users,
  Scale, CreditCard, Heart, Shield,
} from 'lucide-react-native';
import { colors } from '../theme/colors';

interface Props {
  navigation: any;
}

const CATEGORIES = [
  { id: '1', Icon: FileText, title: '계약/문서', count: 234 },
  { id: '2', Icon: Home, title: '부동산/임대차', count: 189 },
  { id: '3', Icon: Briefcase, title: '노동/근로', count: 167 },
  { id: '4', Icon: Users, title: '가족/상속', count: 145 },
  { id: '5', Icon: Scale, title: '민사/형사', count: 198 },
  { id: '6', Icon: CreditCard, title: '금융/채무', count: 123 },
  { id: '7', Icon: Heart, title: '의료/건강', count: 98 },
  { id: '8', Icon: Shield, title: '소비자보호', count: 112 },
];

export function CategoryScreen({ navigation }: Props) {
  // 검색어 상태 관리 추가
  const [searchQuery, setSearchQuery] = useState('');

  // 검색어에 따른 카테고리 필터링 로직
  const filteredCategories = CATEGORIES.filter(category =>
    category.title.includes(searchQuery)
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>법률 카테고리</Text>
        <Text style={styles.subtitle}>분야별로 전문화된 법률 상담을 제공합니다</Text>
      </View>

      <View style={styles.searchBox}>
        <TextInput
          style={styles.searchInput}
          placeholder="카테고리 검색..."
          placeholderTextColor={colors.mutedForeground}
          value={searchQuery}
          onChangeText={setSearchQuery} // 검색어 입력 시 상태 업데이트
          clearButtonMode="while-editing" // iOS x 버튼
        />
      </View>

      <FlatList
        // 필터링된 데이터를 FlatList에 전달
        data={filteredCategories}
        keyExtractor={(item) => item.id}
        numColumns={2}
        contentContainerStyle={styles.grid}
        columnWrapperStyle={styles.row}
        // 검색 결과가 없을 때 보여줄 UI 추가
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>검색 결과가 없습니다.</Text>
          </View>
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => navigation.navigate('Chat', { categoryId: item.id, categoryTitle: item.title })}
          >
            <View style={styles.cardIcon}>
              <item.Icon color={colors.primary} size={24} />
            </View>
            <Text style={styles.cardTitle}>{item.title}</Text>
            <Text style={styles.cardCount}>{item.count}개 상담</Text>
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
  searchBox: { paddingHorizontal: 24, marginBottom: 12 },
  searchInput: {
    paddingHorizontal: 16, paddingVertical: 12,
    backgroundColor: colors.inputBackground,
    borderRadius: 12, fontSize: 14, color: colors.foreground,
  },
  grid: { paddingHorizontal: 24, paddingBottom: 24 },
  row: { gap: 12, marginBottom: 12 },
  card: {
    flex: 1, padding: 16, backgroundColor: colors.white,
    borderRadius: 12, borderWidth: 1, borderColor: colors.border,
  },
  cardIcon: {
    width: 48, height: 48, backgroundColor: colors.primaryLight,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center', marginBottom: 12,
  },
  cardTitle: { fontSize: 14, fontWeight: '600', color: colors.foreground, marginBottom: 4 },
  cardCount: { fontSize: 12, color: colors.mutedForeground },
  emptyContainer: {
    padding: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    fontSize: 14,
    color: colors.mutedForeground,
  },
});