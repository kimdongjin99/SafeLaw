import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ChevronDown, ChevronUp } from 'lucide-react-native';
import { colors } from '../../theme/colors';
import { SubScreenHeader } from '../../components/SubScreenHeader';

interface Props {
  navigation: any;
}

const FAQ: { q: string; a: string }[] = [
  {
    q: 'SafeLaw는 어떤 앱인가요?',
    a: '법률 고민을 입력하면 비슷한 실제 판례를 찾아, 그 판례를 근거로 AI가 답변하는 법률 상담 앱입니다. 질문은 기기 밖으로 나가지 않습니다.',
  },
  {
    q: '내 질문이 서버로 전송되나요?',
    a: '아니요. 질문 분석과 판례 검색, 답변 생성은 모두 기기 안에서 이루어집니다. 서버에는 판례 원문을 받기 위한 판례 번호만 전송되며, 진짜 번호에 가짜 번호를 섞어 보내 어떤 판례를 찾는지 알 수 없게 합니다.',
  },
  {
    q: '처음 질문할 때 왜 오래 걸리나요?',
    a: '설치 후 첫 질문 때 판례 12만여 건을 기기 DB에 준비하고 AI 모델을 불러오느라 시간이 걸립니다. 한 번 준비되면 이후에는 빠르게 답변합니다.',
  },
  {
    q: '답변은 법적 효력이 있나요?',
    a: 'SafeLaw의 답변은 판례를 바탕으로 한 참고용 법률 정보입니다. 실제 사건은 변호사 등 전문가와 상담하시기 바랍니다.',
  },
  {
    q: '인터넷이 없어도 쓸 수 있나요?',
    a: '판례 검색과 답변 생성은 기기에서 동작합니다. 다만 판례 원문을 받아오려면 인터넷이 필요하며, 연결이 안 되면 판례 없이 답변합니다.',
  },
  {
    q: '이전 상담 내용은 어디서 보나요?',
    a: '홈 화면의 "상담 내역 보기"에서 확인할 수 있습니다. 상담 내역은 이 기기에만 저장됩니다.',
  },
  {
    q: '상담 기록을 지우고 싶어요.',
    a: '설정 > 로컬 데이터 관리에서 저장된 상담 내역과 검색 캐시를 한 번에 삭제할 수 있습니다. 상담 내역 화면에서 하나씩 지울 수도 있습니다.',
  },
];

// 도움말: 자주 묻는 질문
export function HelpScreen({ navigation }: Props) {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <SafeAreaView style={styles.container}>
      <SubScreenHeader title="도움말" subtitle="자주 묻는 질문" onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll}>
        {FAQ.map((item, index) => {
          const open = openIndex === index;
          return (
            <TouchableOpacity
              key={item.q}
              style={styles.card}
              activeOpacity={0.8}
              onPress={() => setOpenIndex(open ? null : index)}
            >
              <View style={styles.qRow}>
                <Text style={styles.qMark}>Q</Text>
                <Text style={styles.question}>{item.q}</Text>
                {open ? (
                  <ChevronUp color={colors.mutedForeground} size={18} />
                ) : (
                  <ChevronDown color={colors.mutedForeground} size={18} />
                )}
              </View>
              {open ? <Text style={styles.answer}>{item.a}</Text> : null}
            </TouchableOpacity>
          );
        })}
        <Text style={styles.footer}>
          더 궁금한 점은 졸업프로젝트 SafeLaw 팀에 문의해주세요.
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 32 },
  card: {
    padding: 16, marginBottom: 10,
    borderWidth: 1, borderColor: colors.border, borderRadius: 12,
  },
  qRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  qMark: { fontSize: 15, fontWeight: '700', color: colors.primary },
  question: { flex: 1, fontSize: 14, fontWeight: '600', color: colors.foreground },
  answer: { marginTop: 10, fontSize: 13, color: colors.mutedForeground, lineHeight: 20 },
  footer: { marginTop: 12, fontSize: 12, color: colors.mutedForeground, textAlign: 'center' },
});
