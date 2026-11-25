import React, { useState } from 'react';
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
import { Eye, EyeOff } from 'lucide-react-native';
import { colors } from '../../theme/colors';
import apiClient from '../../api/client';
import { SubScreenHeader } from '../../components/SubScreenHeader';

interface Props {
  navigation: any;
}

// 서버 AccountValidator와 같은 기준
const MIN_PASSWORD_LENGTH = 8;
const MAX_PASSWORD_LENGTH = 64;

// 비밀번호 변경 (PUT /api/v1/users/me/password) - 이메일 가입자만 설정 화면에 메뉴가 보인다
export function ChangePasswordScreen({ navigation }: Props) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const lengthOk = next.length >= MIN_PASSWORD_LENGTH && next.length <= MAX_PASSWORD_LENGTH;
  const matchOk = next.length > 0 && next === confirm;
  const differentOk = next.length > 0 && next !== current;
  const canSave = !isSaving && current.length > 0 && lengthOk && matchOk && differentOk;

  const handleSave = async () => {
    if (!canSave) return;
    setIsSaving(true);
    try {
      await apiClient.put('/api/v1/users/me/password', {
        currentPassword: current,
        newPassword: next,
      });
      Alert.alert('완료', '비밀번호가 변경되었습니다.', [
        { text: '확인', onPress: () => navigation.goBack() },
      ]);
    } catch (error: any) {
      const code = error.response?.data?.code;
      let message = error.response?.data?.message || '비밀번호를 변경하지 못했습니다.';
      if (code === 'INVALID_CURRENT_PASSWORD') {
        message = '현재 비밀번호가 올바르지 않습니다.';
      } else if (code === 'PASSWORD_NOT_SET') {
        message = '소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.';
      }
      Alert.alert('오류', message);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <SubScreenHeader title="비밀번호 변경" subtitle="계정 비밀번호 수정" onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <PasswordField
          label="현재 비밀번호"
          value={current}
          onChange={setCurrent}
          visible={showPassword}
          onToggle={() => setShowPassword((v) => !v)}
        />
        <PasswordField
          label="새 비밀번호"
          value={next}
          onChange={setNext}
          visible={showPassword}
          onToggle={() => setShowPassword((v) => !v)}
        />
        <PasswordField
          label="새 비밀번호 확인"
          value={confirm}
          onChange={setConfirm}
          visible={showPassword}
          onToggle={() => setShowPassword((v) => !v)}
        />

        <View style={styles.rules}>
          <Rule ok={lengthOk} text={`${MIN_PASSWORD_LENGTH}~${MAX_PASSWORD_LENGTH}자`} />
          <Rule ok={matchOk} text="새 비밀번호와 확인이 일치" />
          <Rule ok={differentOk} text="현재 비밀번호와 다름" />
        </View>

        <TouchableOpacity
          style={[styles.saveBtn, !canSave && styles.saveBtnDisabled]}
          onPress={handleSave}
          disabled={!canSave}
        >
          {isSaving ? (
            <ActivityIndicator color={colors.white} />
          ) : (
            <Text style={styles.saveBtnText}>비밀번호 변경</Text>
          )}
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

function PasswordField({
  label,
  value,
  onChange,
  visible,
  onToggle,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  visible: boolean;
  onToggle: () => void;
}) {
  return (
    <View style={{ marginBottom: 16 }}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          value={value}
          onChangeText={onChange}
          secureTextEntry={!visible}
          autoCapitalize="none"
          autoCorrect={false}
          maxLength={MAX_PASSWORD_LENGTH}
          placeholder={label}
          placeholderTextColor={colors.mutedForeground}
        />
        <TouchableOpacity onPress={onToggle} hitSlop={10}>
          {visible ? (
            <EyeOff color={colors.mutedForeground} size={20} />
          ) : (
            <Eye color={colors.mutedForeground} size={20} />
          )}
        </TouchableOpacity>
      </View>
    </View>
  );
}

function Rule({ ok, text }: { ok: boolean; text: string }) {
  return (
    <Text style={[styles.rule, ok && styles.ruleOk]}>
      {ok ? '✓ ' : '• '}
      {text}
    </Text>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 32, paddingTop: 8 },
  label: { fontSize: 14, fontWeight: '600', color: colors.foreground, marginBottom: 8 },
  inputRow: {
    flexDirection: 'row', alignItems: 'center',
    backgroundColor: colors.inputBackground, borderRadius: 12, paddingHorizontal: 16,
  },
  input: { flex: 1, paddingVertical: 14, fontSize: 15, color: colors.foreground },
  rules: { marginTop: 4, gap: 6 },
  rule: { fontSize: 13, color: colors.mutedForeground },
  ruleOk: { color: colors.primary },
  saveBtn: {
    marginTop: 28, backgroundColor: colors.primary, paddingVertical: 16,
    borderRadius: 12, alignItems: 'center',
  },
  saveBtnDisabled: { opacity: 0.4 },
  saveBtnText: { color: colors.white, fontSize: 16, fontWeight: '600' },
});
