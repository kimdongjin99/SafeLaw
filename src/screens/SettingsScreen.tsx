import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  User, Bell, Shield, HelpCircle,
  Info, ChevronRight, Database, LogOut
} from 'lucide-react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { colors } from '../theme/colors';
import apiClient from '../api/client';

interface Props {
  navigation: any;
}

export function SettingsScreen({ navigation }: Props) {
  const [userData, setUserData] = useState({ name: '사용자', email: '로딩 중...' });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const response = await apiClient.get('/api/v1/users/me');
        setUserData({ name: response.data.name, email: response.data.email });
      } catch (error: any) {
        console.error('사용자 정보 조회 실패:', error);
        
        if (error.response?.status === 401) {
          Alert.alert('알림', '세션이 만료되었습니다. 다시 로그인해주세요.');
          navigation.replace('Login');
          return;
        }
        
        setUserData({ name: '알 수 없음', email: '정보를 불러오지 못했습니다.' });
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserInfo();
  }, [navigation]);

  const handleLogout = () => {
    Alert.alert('로그아웃', '정말 로그아웃 하시겠습니까?', [
      { text: '취소', style: 'cancel' },
      { 
        text: '로그아웃', 
        style: 'destructive', 
        onPress: async () => {
          // 보안 저장소에서 토큰 삭제 후 로그인 화면으로 이동
          await EncryptedStorage.removeItem('accessToken');
          navigation.replace('Login');
        }
      }
    ]);
  };

  const handleClearLocalData = () => {
    Alert.alert(
      '로컬 데이터 삭제',
      '기기에 저장된 모든 상담 내역이 영구적으로 삭제됩니다. 계속하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        { 
          text: '삭제', 
          style: 'destructive', 
          onPress: () => console.log('로컬 DB 삭제 수행') 
        }
      ]
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.header}>
          <Text style={styles.title}>설정</Text>
          <Text style={styles.subtitle}>앱 설정 및 개인정보 관리</Text>
        </View>

        <View style={styles.profileCard}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {isLoading ? '...' : userData.name.charAt(0)}
            </Text>
          </View>
          <View style={{ flex: 1 }}>
            {isLoading ? (
              <ActivityIndicator size="small" color={colors.primary} style={{ alignItems: 'flex-start' }} />
            ) : (
              <>
                <Text style={styles.profileName}>{userData.name}</Text>
                <Text style={styles.profileEmail}>{userData.email}</Text>
              </>
            )}
          </View>
        </View>

        <SettingsSection title="계정 설정">
          <SettingItem
            icon={<User color={colors.foreground} size={20} />}
            label="프로필 관리"
            description="이름, 비밀번호 등 정보 수정"
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
            description="저장된 상담 내역 및 캐시 삭제"
            onPress={handleClearLocalData}
          />
        </SettingsSection>

        <SettingsSection title="지원 및 기타">
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
          <SettingItem
            icon={<LogOut color={colors.destructive || '#FF3B30'} size={20} />}
            label="로그아웃"
            description="계정에서 로그아웃 합니다"
            onPress={handleLogout}
            isDestructive
          />
        </SettingsSection>

        <View style={styles.privacyBox}>
          <Shield color={colors.primary} size={20} />
          <Text style={styles.privacyText}>
            모든 상담 내용은 기기 내에서만 처리되며, 서버나 외부로 전송되지 않습니다.
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
  onPress,
  isDestructive,
}: {
  icon: React.ReactNode;
  label: string;
  description: string;
  onPress?: () => void;
  isDestructive?: boolean;
}) {
  return (
    <TouchableOpacity 
      style={styles.settingItem} 
      onPress={onPress} 
      activeOpacity={onPress ? 0.7 : 1}
    >
      <View style={[styles.settingIcon, isDestructive && { backgroundColor: '#FFEBEB' }]}>
        {icon}
      </View>
      <View style={{ flex: 1 }}>
        <Text style={[styles.settingLabel, isDestructive && { color: '#FF3B30' }]}>{label}</Text>
        <Text style={styles.settingDesc}>{description}</Text>
      </View>
      {onPress && <ChevronRight color={colors.mutedForeground} size={20} />}
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
    marginTop: 8,
  },
  privacyText: { fontSize: 12, color: colors.foreground, flex: 1, lineHeight: 18 },
});