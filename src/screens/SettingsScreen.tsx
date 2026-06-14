import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  User, Bell, Shield, HelpCircle,
  Info, ChevronRight, Database,
} from 'lucide-react-native';
import { colors } from '../theme/colors';

export function SettingsScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.header}>
          <Text style={styles.title}>설정</Text>
          <Text style={styles.subtitle}>앱 설정 및 개인정보 관리</Text>
        </View>

        {/* Profile */}
        <View style={styles.profileCard}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>사</Text>
          </View>
          <View>
            <Text style={styles.profileName}>사용자</Text>
            <Text style={styles.profileEmail}>user@example.com</Text>
          </View>
        </View>

        <SettingsSection title="계정 설정">
          <SettingItem
            icon={<User color={colors.foreground} size={20} />}
            label="프로필 관리"
            description="이름, 이메일 등 정보 수정"
          />
          <SettingItem
            icon={<Bell color={colors.foreground} size={20} />}
            label="알림 설정"
            description="푸시 알림 및 이메일 알림"
          />
        </SettingsSection>

        <SettingsSection title="프라이버시 및 보안">
          <SettingItem
            icon={<Shield color={colors.foreground} size={20} />}
            label="데이터 보안"
            description="온디바이스 AI 처리 설정"
          />
          <SettingItem
            icon={<Database color={colors.foreground} size={20} />}
            label="로컬 데이터 관리"
            description="저장된 상담 내역 관리"
          />
        </SettingsSection>

        <SettingsSection title="지원">
          <SettingItem
            icon={<HelpCircle color={colors.foreground} size={20} />}
            label="도움말"
            description="자주 묻는 질문"
          />
          <SettingItem
            icon={<Info color={colors.foreground} size={20} />}
            label="앱 정보"
            description="버전 1.0.0"
          />
        </SettingsSection>

        {/* Privacy Notice */}
        <View style={styles.privacyBox}>
          <Shield color={colors.primary} size={20} />
          <Text style={styles.privacyText}>
            모든 상담 내용은 기기 내에서만 처리되며, 외부로 전송되지 않습니다.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function SettingsSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <View style={styles.sectionItems}>{children}</View>
    </View>
  );
}

function SettingItem({
  icon,
  label,
  description,
}: {
  icon: React.ReactNode;
  label: string;
  description: string;
}) {
  return (
    <TouchableOpacity style={styles.settingItem}>
      <View style={styles.settingIcon}>{icon}</View>
      <View style={{ flex: 1 }}>
        <Text style={styles.settingLabel}>{label}</Text>
        <Text style={styles.settingDesc}>{description}</Text>
      </View>
      <ChevronRight color={colors.mutedForeground} size={20} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 32 },
  header: { paddingTop: 32, paddingBottom: 16 },
  title: { fontSize: 26, fontWeight: '700', color: colors.foreground, marginBottom: 6 },
  subtitle: { fontSize: 14, color: colors.mutedForeground },
  profileCard: {
    flexDirection: 'row', alignItems: 'center', gap: 16,
    padding: 16, backgroundColor: colors.secondary,
    borderRadius: 12, marginBottom: 24,
  },
  avatar: {
    width: 56, height: 56, borderRadius: 28,
    backgroundColor: colors.primary,
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { color: colors.white, fontSize: 18, fontWeight: '600' },
  profileName: { fontSize: 16, fontWeight: '600', color: colors.foreground, marginBottom: 4 },
  profileEmail: { fontSize: 13, color: colors.mutedForeground },
  section: { marginBottom: 24 },
  sectionTitle: {
    fontSize: 13, color: colors.mutedForeground,
    marginBottom: 10, paddingHorizontal: 4,
  },
  sectionItems: { gap: 8 },
  settingItem: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    padding: 16, backgroundColor: colors.white,
    borderWidth: 1, borderColor: colors.border, borderRadius: 12,
  },
  settingIcon: {
    width: 40, height: 40, backgroundColor: colors.secondary,
    borderRadius: 8, alignItems: 'center', justifyContent: 'center',
  },
  settingLabel: { fontSize: 14, fontWeight: '500', color: colors.foreground, marginBottom: 2 },
  settingDesc: { fontSize: 12, color: colors.mutedForeground },
  privacyBox: {
    flexDirection: 'row', alignItems: 'flex-start', gap: 10,
    padding: 14, backgroundColor: colors.primaryLight, borderRadius: 10,
  },
  privacyText: { fontSize: 12, color: colors.foreground, flex: 1, lineHeight: 18 },
});