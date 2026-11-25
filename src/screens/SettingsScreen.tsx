import React, { useCallback, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  ActivityIndicator,
  NativeModules,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  User, Bell, Shield, HelpCircle,
  Info, ChevronRight, Database, LogOut, Lock
} from 'lucide-react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { GoogleSignin } from '@react-native-google-signin/google-signin';
import { colors } from '../theme/colors';
import apiClient from '../api/client';
import { clearAllChatSessions, getChatSessions } from '../services/chatStorage';

const safeLawModule = NativeModules.SafeLawModule as
  | { clearSearchCache?: () => Promise<boolean> }
  | undefined;

interface Props {
  navigation: any;
}

export function SettingsScreen({ navigation }: Props) {
  // provider 필드 추가 및 기본 상태 설정
  const [userData, setUserData] = useState({ 
    name: '사용자', 
    email: '로딩 중...',
    provider: 'EMAIL' 
  });
  const [isLoading, setIsLoading] = useState(true);

  // 프로필 수정 후 돌아왔을 때도 새 이름이 보이도록, 화면에 들어올 때마다 불러온다
  useFocusEffect(useCallback(() => {
    const fetchUserInfo = async () => {
      try {
        const response = await apiClient.get('/api/v1/users/me');
        setUserData({ 
          name: response.data.name, 
          // 소셜 로그인은 email이 null일 수 있으므로 null 병합 처리[cite: 17]
          email: response.data.email ?? '소셜 계정 로그인',
          provider: response.data.provider
        });
      } catch (error: any) {
        console.error('사용자 정보 조회 실패:', error);
        
        if (error.response?.status === 401) {
          Alert.alert('알림', '세션이 만료되었습니다. 다시 로그인해주세요.');
          navigation.replace('Login');
          return;
        }
        
        setUserData({ name: '알 수 없음', email: '정보를 불러오지 못했습니다.', provider: 'EMAIL' });
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserInfo();
  }, [navigation]));

  const handleLogout = () => {
    Alert.alert('로그아웃', '정말 로그아웃 하시겠습니까?', [
      { text: '취소', style: 'cancel' },
      { 
        text: '로그아웃', 
        style: 'destructive', 
        onPress: async () => {
          try {
            // 1. 백엔드 로그아웃 API 호출[cite: 15]
            await apiClient.post('/api/v1/auth/logout');
          } catch (error: any) {
            // 401(UNAUTHORIZED) 등 에러가 발생해도 정상 로그아웃 흐름 동일 진행[cite: 16]
            console.log('Logout API Status:', error.response?.status || error.message);
          } finally {
            // 2. 보안 저장소에서 토큰 삭제[cite: 16]
            await EncryptedStorage.removeItem('accessToken');
            
            // 3. Google 소셜 로그인 세션 초기화 (선택적 예외 처리)
            try {
              await GoogleSignin.signOut();
            } catch (e) {
              console.log('Google SignOut Error:', e);
            }

            // 4. 로그인 화면으로 이동[cite: 16]
            navigation.replace('Login');
          }
        }
      }
    ]);
  };

  // 로컬 데이터 관리: 기기에 저장된 상담 내역과 검색 캐시를 지운다 (판례 DB와 AI 모델은 유지)
  const handleClearLocalData = async () => {
    const sessions = await getChatSessions();
    const messageCount = sessions.reduce((sum, s) => sum + s.messages.length, 0);
    Alert.alert(
      '로컬 데이터 삭제',
      `기기에 저장된 상담 ${sessions.length}건(메시지 ${messageCount}개)과 검색 캐시가 영구적으로 삭제됩니다. 계속하시겠습니까?\n\n판례 데이터와 AI 모델은 삭제되지 않습니다.`,
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: async () => {
            try {
              await clearAllChatSessions();
              await safeLawModule?.clearSearchCache?.();
              Alert.alert('완료', '저장된 상담 내역과 검색 캐시를 삭제했습니다.');
            } catch (error) {
              console.error('로컬 데이터 삭제 실패:', error);
              Alert.alert('오류', '삭제 중 문제가 발생했습니다. 다시 시도해주세요.');
            }
          },
        },
      ]
    );
  };

  const handleAppInfo = () => {
    Alert.alert(
      'SafeLaw',
      '버전 1.0.0\n온디바이스 AI 기반 개인정보 보호형 법률 상담 앱\n\n임베딩: KR-SBERT · 검색: ObjectBox HNSW\n답변 생성: EXAONE (온디바이스)\n\n2026 졸업프로젝트 SafeLaw 팀'
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
            description="이름 등 정보 수정"
            onPress={() => navigation.navigate('Profile')}
          />
          {/* EMAIL 가입자인 경우에만 비밀번호 변경 메뉴 노출[cite: 17] */}
          {userData.provider === 'EMAIL' && (
            <SettingItem
              icon={<Lock color={colors.foreground} size={20} />}
              label="비밀번호 변경"
              description="계정 비밀번호 수정"
              onPress={() => navigation.navigate('ChangePassword')}
            />
          )}
          <SettingItem
            icon={<Bell color={colors.foreground} size={20} />}
            label="알림 설정"
            description="앱 알림 및 이메일 알림"
            onPress={() => navigation.navigate('NotificationSettings')}
          />
        </SettingsSection>

        <SettingsSection title="프라이버시 및 보안">
          <SettingItem
            icon={<Shield color={colors.foreground} size={20} />}
            label="데이터 보안"
            description="온디바이스 AI 처리 방식"
            onPress={() => navigation.navigate('DataSecurity')}
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
            onPress={() => navigation.navigate('Help')}
          />
          <SettingItem
            icon={<Info color={colors.foreground} size={20} />}
            label="앱 정보"
            description="버전 1.0.0"
            onPress={handleAppInfo}
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