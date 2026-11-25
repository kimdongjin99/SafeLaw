import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../../theme/colors';
import apiClient from '../../api/client';
import { SubScreenHeader } from '../../components/SubScreenHeader';

interface Props {
  navigation: any;
}

const MAX_NAME_LENGTH = 30; // 서버 AccountValidator와 같은 기준

const PROVIDER_LABEL: Record<string, string> = {
  EMAIL: '이메일',
  GOOGLE: 'Google',
  APPLE: 'Apple',
};

// 프로필 관리: 이름 변경 (PATCH /api/v1/users/me)
export function ProfileScreen({ navigation }: Props) {
  const [name, setName] = useState('');
  const [originalName, setOriginalName] = useState('');
  const [email, setEmail] = useState<string | null>(null);
  const [provider, setProvider] = useState('EMAIL');
  const [createdAt, setCreatedAt] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await apiClient.get('/api/v1/users/me');
        setName(res.data.name ?? '');
        setOriginalName(res.data.name ?? '');
        setEmail(res.data.email ?? null);
        setProvider(res.data.provider ?? 'EMAIL');
        setCreatedAt(res.data.createdAt ?? null);
      } catch (error: any) {
        if (error.response?.status === 401) {
          Alert.alert('알림', '세션이 만료되었습니다. 다시 로그인해주세요.');
          navigation.replace('Login');
          return;
        }
        Alert.alert('오류', '회원 정보를 불러오지 못했습니다.');
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [navigation]);

  const trimmed = name.trim();
  const canSave =
    !isSaving &&
    trimmed.length > 0 &&
    trimmed.length <= MAX_NAME_LENGTH &&
    trimmed !== originalName;

  const handleSave = async () => {
    if (!canSave) return;
    setIsSaving(true);
    try {
      const res = await apiClient.patch('/api/v1/users/me', { name: trimmed });
      setOriginalName(res.data.name ?? trimmed);
      Alert.alert('완료', '이름이 변경되었습니다.', [
        { text: '확인', onPress: () => navigation.goBack() },
      ]);
    } catch (error: any) {
      const message =
        error.response?.data?.message || '이름을 변경하지 못했습니다. 잠시 후 다시 시도해주세요.';
      Alert.alert('오류', message);
    } finally {
      setIsSaving(false);
    }
  };

  const formatDate = (iso: string | null) => {
    if (!iso) return '-';
    const d = new Date(iso);
    return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`;
  };

  return (
    <SafeAreaView style={styles.container}>
      <SubScreenHeader title="프로필 관리" subtitle="이름 등 계정 정보" onBack={() => navigation.goBack()} />
      {isLoading ? (
        <ActivityIndicator style={{ marginTop: 40 }} color={colors.primary} />
      ) : (
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <View style={styles.avatarWrap}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>{(trimmed || originalName || '?').charAt(0)}</Text>
            </View>
          </View>

          <Text style={styles.label}>이름</Text>
          <TextInput
            style={styles.input}
            value={name}
            onChangeText={setName}
            maxLength={MAX_NAME_LENGTH}
            placeholder="이름을 입력하세요"
            placeholderTextColor={colors.mutedForeground}
          />
          <Text style={styles.hint}>
            {trimmed.length}/{MAX_NAME_LENGTH}자
          </Text>

          <View style={styles.infoBox}>
            <InfoRow label="이메일" value={email ?? '소셜 계정 로그인'} />
            <InfoRow label="가입 방식" value={PROVIDER_LABEL[provider] ?? provider} />
            <InfoRow label="가입일" value={formatDate(createdAt)} />
          </View>

          <TouchableOpacity
            style={[styles.saveBtn, !canSave && styles.saveBtnDisabled]}
            onPress={handleSave}
            disabled={!canSave}
          >
            {isSaving ? (
              <ActivityIndicator color={colors.white} />
            ) : (
              <Text style={styles.saveBtnText}>저장</Text>
            )}
          </TouchableOpacity>
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.infoRow}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 32 },
  avatarWrap: { alignItems: 'center', marginVertical: 16 },
  avatar: {
    width: 80, height: 80, borderRadius: 40,
    backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { color: colors.white, fontSize: 28, fontWeight: '700' },
  label: { fontSize: 14, fontWeight: '600', color: colors.foreground, marginBottom: 8 },
  input: {
    backgroundColor: colors.inputBackground, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, color: colors.foreground,
  },
  hint: { fontSize: 12, color: colors.mutedForeground, textAlign: 'right', marginTop: 6 },
  infoBox: {
    marginTop: 20, padding: 16, borderRadius: 12,
    borderWidth: 1, borderColor: colors.border, gap: 12,
  },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between' },
  infoLabel: { fontSize: 13, color: colors.mutedForeground },
  infoValue: { fontSize: 13, color: colors.foreground, fontWeight: '500' },
  saveBtn: {
    marginTop: 28, backgroundColor: colors.primary, paddingVertical: 16,
    borderRadius: 12, alignItems: 'center',
  },
  saveBtnDisabled: { opacity: 0.4 },
  saveBtnText: { color: colors.white, fontSize: 16, fontWeight: '600' },
});
