import React, { useEffect, useState } from 'react';
import { View, Text, Switch, StyleSheet, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Bell } from 'lucide-react-native';
import { colors } from '../../theme/colors';
import { SubScreenHeader } from '../../components/SubScreenHeader';

interface Props {
  navigation: any;
}

const STORAGE_KEY = '@safelaw_notification_settings';

interface NotificationSettings {
  answerDone: boolean;
  modelUpdate: boolean;
  serviceNotice: boolean;
  emailNews: boolean;
}

const DEFAULT_SETTINGS: NotificationSettings = {
  answerDone: true,
  modelUpdate: true,
  serviceNotice: true,
  emailNews: false,
};

// 알림 설정: 기기에 저장되는 알림 수신 선택
export function NotificationSettingsScreen({ navigation }: Props) {
  const [settings, setSettings] = useState<NotificationSettings>(DEFAULT_SETTINGS);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((json) => {
        if (json) setSettings({ ...DEFAULT_SETTINGS, ...JSON.parse(json) });
      })
      .catch(() => {});
  }, []);

  const update = (key: keyof NotificationSettings, value: boolean) => {
    const next = { ...settings, [key]: value };
    setSettings(next);
    AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next)).catch(() => {});
  };

  return (
    <SafeAreaView style={styles.container}>
      <SubScreenHeader title="알림 설정" subtitle="받을 알림을 선택하세요" onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.sectionTitle}>앱 알림</Text>
        <ToggleRow
          label="답변 완료 알림"
          description="AI 답변 생성이 끝나면 알려드려요"
          value={settings.answerDone}
          onChange={(v) => update('answerDone', v)}
        />
        <ToggleRow
          label="AI 모델 업데이트 알림"
          description="새 법률 AI 모델이 나오면 알려드려요"
          value={settings.modelUpdate}
          onChange={(v) => update('modelUpdate', v)}
        />
        <ToggleRow
          label="서비스 공지"
          description="점검, 판례 데이터 갱신 등 안내"
          value={settings.serviceNotice}
          onChange={(v) => update('serviceNotice', v)}
        />

        <Text style={[styles.sectionTitle, { marginTop: 24 }]}>이메일</Text>
        <ToggleRow
          label="이메일 소식 받기"
          description="새 기능과 법률 정보 소식을 이메일로 받아요"
          value={settings.emailNews}
          onChange={(v) => update('emailNews', v)}
        />

        <View style={styles.noteBox}>
          <Bell color={colors.primary} size={18} />
          <Text style={styles.noteText}>
            알림 설정은 이 기기에만 저장됩니다. 상담 내용은 알림에 포함되지 않습니다.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function ToggleRow({
  label,
  description,
  value,
  onChange,
}: {
  label: string;
  description: string;
  value: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <View style={styles.row}>
      <View style={{ flex: 1 }}>
        <Text style={styles.rowLabel}>{label}</Text>
        <Text style={styles.rowDesc}>{description}</Text>
      </View>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ false: colors.border, true: colors.primary }}
        thumbColor={colors.white}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 32 },
  sectionTitle: { fontSize: 13, color: colors.mutedForeground, marginBottom: 10, paddingHorizontal: 4 },
  row: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    padding: 16, marginBottom: 8,
    borderWidth: 1, borderColor: colors.border, borderRadius: 12,
  },
  rowLabel: { fontSize: 14, fontWeight: '500', color: colors.foreground, marginBottom: 2 },
  rowDesc: { fontSize: 12, color: colors.mutedForeground },
  noteBox: {
    flexDirection: 'row', gap: 10, alignItems: 'flex-start',
    padding: 14, backgroundColor: colors.primaryLight, borderRadius: 10, marginTop: 16,
  },
  noteText: { flex: 1, fontSize: 12, color: colors.foreground, lineHeight: 18 },
});
