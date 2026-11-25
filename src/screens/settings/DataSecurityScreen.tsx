import React, { useCallback, useState } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { Smartphone, Shuffle, Database, Server, Lock } from 'lucide-react-native';
import { colors } from '../../theme/colors';
import { SubScreenHeader } from '../../components/SubScreenHeader';
import { getChatSessions } from '../../services/chatStorage';

interface Props {
  navigation: any;
}

// 데이터 보안: 앱이 개인정보를 어떻게 다루는지 설명하는 화면
export function DataSecurityScreen({ navigation }: Props) {
  const [sessionCount, setSessionCount] = useState(0);
  const [messageCount, setMessageCount] = useState(0);

  useFocusEffect(
    useCallback(() => {
      getChatSessions().then((sessions) => {
        setSessionCount(sessions.length);
        setMessageCount(sessions.reduce((sum, s) => sum + s.messages.length, 0));
      });
    }, [])
  );

  return (
    <SafeAreaView style={styles.container}>
      <SubScreenHeader title="데이터 보안" subtitle="온디바이스 AI 처리 방식" onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.statusCard}>
          <Lock color={colors.white} size={22} />
          <View style={{ flex: 1 }}>
            <Text style={styles.statusTitle}>온디바이스 보안 모드 작동 중</Text>
            <Text style={styles.statusDesc}>질문 내용은 이 기기 밖으로 나가지 않습니다</Text>
          </View>
        </View>

        <SecurityItem
          icon={<Smartphone color={colors.primary} size={20} />}
          title="질문 분석은 기기 안에서"
          body="질문을 문장 벡터로 바꾸는 임베딩(KR-SBERT)과 판례 검색(ObjectBox HNSW), 답변 생성(EXAONE)이 모두 이 기기에서 실행됩니다."
        />
        <SecurityItem
          icon={<Shuffle color={colors.primary} size={20} />}
          title="섀도우 쿼리로 판례 요청 숨기기"
          body="판례 원문을 받을 때 실제 판례 번호에 무작위 가짜 번호 10개를 섞어 요청합니다. 서버는 어떤 판례가 실제로 필요한지 알 수 없습니다."
        />
        <SecurityItem
          icon={<Server color={colors.primary} size={20} />}
          title="서버가 받는 정보"
          body="서버에는 판례 번호 묶음만 전송됩니다. 질문 내용, 질문 벡터, AI 답변은 전송되지 않습니다."
        />
        <SecurityItem
          icon={<Database color={colors.primary} size={20} />}
          title="상담 기록은 기기에만 저장"
          body={`현재 이 기기에 상담 ${sessionCount}건, 메시지 ${messageCount}개가 저장되어 있습니다. 설정 > 로컬 데이터 관리에서 언제든 삭제할 수 있습니다.`}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

function SecurityItem({
  icon,
  title,
  body,
}: {
  icon: React.ReactNode;
  title: string;
  body: string;
}) {
  return (
    <View style={styles.item}>
      <View style={styles.itemIcon}>{icon}</View>
      <View style={{ flex: 1 }}>
        <Text style={styles.itemTitle}>{title}</Text>
        <Text style={styles.itemBody}>{body}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 32 },
  statusCard: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    padding: 16, borderRadius: 12, backgroundColor: colors.primary, marginBottom: 16,
  },
  statusTitle: { color: colors.white, fontSize: 15, fontWeight: '700' },
  statusDesc: { color: colors.primaryLight, fontSize: 12, marginTop: 2 },
  item: {
    flexDirection: 'row', gap: 12, padding: 16, marginBottom: 10,
    borderWidth: 1, borderColor: colors.border, borderRadius: 12,
  },
  itemIcon: {
    width: 40, height: 40, borderRadius: 8, backgroundColor: colors.primaryLight,
    alignItems: 'center', justifyContent: 'center',
  },
  itemTitle: { fontSize: 14, fontWeight: '600', color: colors.foreground, marginBottom: 4 },
  itemBody: { fontSize: 13, color: colors.mutedForeground, lineHeight: 19 },
});
