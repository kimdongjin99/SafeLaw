import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Shield, Mail, Lock, Eye, EyeOff, AlertCircle } from 'lucide-react-native';
import { colors } from '../theme/colors';

const VALID_EMAIL = 'test@email.com';
const VALID_PASSWORD = 'password123';

interface Props {
  onLogin: () => void;
  navigation: any;
}

export function LoginScreen({ onLogin, navigation }: Props) {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loginAttempts, setLoginAttempts] = useState(0);

  const handleLogin = () => {
    if (!email || !password) return;

    if (email === VALID_EMAIL && password === VALID_PASSWORD) {
      setError(null);
      onLogin();
    } else {
      const attempts = loginAttempts + 1;
      setLoginAttempts(attempts);
      if (email !== VALID_EMAIL) {
        setError('등록되지 않은 이메일 주소입니다.');
      } else {
        setError(
          attempts >= 3
            ? `비밀번호가 올바르지 않습니다. (${attempts}회 실패) 계속 실패 시 계정이 잠길 수 있습니다.`
            : '비밀번호가 올바르지 않습니다. 다시 확인해주세요.'
        );
      }
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        <ScrollView
          contentContainerStyle={styles.scroll}
          keyboardShouldPersistTaps="handled"
        >
          {/* Header */}
          <View style={styles.header}>
            <View style={styles.logoBox}>
              <Shield color={colors.white} size={40} />
            </View>
            <Text style={styles.title}>환영합니다</Text>
            <Text style={styles.subtitle}>안전한 법률 상담을 시작하세요</Text>
          </View>

          {/* Form */}
          <View style={styles.form}>
            {/* Email */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>이메일</Text>
              <View style={styles.inputRow}>
                <Mail color={colors.mutedForeground} size={20} />
                <TextInput
                  style={styles.input}
                  value={email}
                  onChangeText={(v) => { setEmail(v); setError(null); }}
                  placeholder="example@email.com"
                  placeholderTextColor={colors.mutedForeground}
                  keyboardType="email-address"
                  autoCapitalize="none"
                />
              </View>
            </View>

            {/* Password */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>비밀번호</Text>
              <View style={[styles.inputRow, error ? styles.inputError : null]}>
                <Lock color={error ? colors.red400 : colors.mutedForeground} size={20} />
                <TextInput
                  style={styles.input}
                  value={password}
                  onChangeText={(v) => { setPassword(v); setError(null); }}
                  placeholder="비밀번호를 입력하세요"
                  placeholderTextColor={colors.mutedForeground}
                  secureTextEntry={!showPassword}
                />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
                  {showPassword
                    ? <EyeOff color={colors.mutedForeground} size={20} />
                    : <Eye color={colors.mutedForeground} size={20} />}
                </TouchableOpacity>
              </View>
              {error && (
                <View style={styles.errorRow}>
                  <AlertCircle color={colors.red500} size={14} />
                  <Text style={styles.errorText}>{error}</Text>
                </View>
              )}
            </View>

            {/* Forgot Password */}
            <TouchableOpacity style={styles.forgotBtn}>
              <Text style={styles.forgotText}>비밀번호를 잊으셨나요?</Text>
            </TouchableOpacity>

            {/* Login Button */}
            <TouchableOpacity
              style={[styles.primaryBtn, (!email || !password) && styles.disabledBtn]}
              onPress={handleLogin}
              disabled={!email || !password}
            >
              <Text style={styles.primaryBtnText}>로그인</Text>
            </TouchableOpacity>

            {/* Divider */}
            <View style={styles.divider}>
              <View style={styles.dividerLine} />
              <Text style={styles.dividerText}>또는</Text>
              <View style={styles.dividerLine} />
            </View>

            {/* Social Login */}
            <TouchableOpacity style={styles.socialBtn}>
              <Text style={styles.socialIcon}>🍎</Text>
              <Text style={styles.socialText}>Apple로 계속하기</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.socialBtn, { marginTop: 10 }]}>
              <Text style={styles.socialIcon}>G</Text>
              <Text style={styles.socialText}>Google로 계속하기</Text>
            </TouchableOpacity>
          </View>

          {/* Privacy Notice */}
          <View style={styles.privacySection}>
            <View style={styles.privacyBox}>
              <Shield color={colors.primary} size={16} />
              <Text style={styles.privacyText}>
                모든 개인정보는 암호화되어 기기에 안전하게 저장됩니다
              </Text>
            </View>
            <View style={styles.signupRow}>
              <Text style={styles.signupLabel}>계정이 없으신가요? </Text>
              <TouchableOpacity onPress={() => navigation.navigate('Signup')}>
                <Text style={styles.signupLink}>회원가입</Text>
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 32 },
  header: { alignItems: 'center', paddingTop: 60, paddingBottom: 32 },
  logoBox: {
    width: 80, height: 80, backgroundColor: colors.primary,
    borderRadius: 20, alignItems: 'center', justifyContent: 'center', marginBottom: 20,
  },
  title: { fontSize: 28, fontWeight: '700', color: colors.foreground, marginBottom: 8 },
  subtitle: { fontSize: 14, color: colors.mutedForeground },
  form: { gap: 16 },
  fieldGroup: { gap: 8 },
  label: { fontSize: 14, color: colors.foreground, paddingHorizontal: 4 },
  inputRow: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    paddingHorizontal: 16, paddingVertical: 14,
    backgroundColor: colors.inputBackground, borderRadius: 12,
    borderWidth: 1, borderColor: 'transparent',
  },
  inputError: { backgroundColor: colors.red50, borderColor: colors.red300 },
  input: { flex: 1, fontSize: 14, color: colors.foreground },
  errorRow: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 4 },
  errorText: { fontSize: 12, color: colors.red500, flex: 1 },
  forgotBtn: { alignItems: 'flex-end' },
  forgotText: { fontSize: 13, color: colors.primary },
  primaryBtn: {
    backgroundColor: colors.primary, paddingVertical: 16,
    borderRadius: 12, alignItems: 'center', marginTop: 4,
  },
  disabledBtn: { opacity: 0.5 },
  primaryBtnText: { color: colors.white, fontSize: 16, fontWeight: '600' },
  divider: { flexDirection: 'row', alignItems: 'center', gap: 12, marginVertical: 8 },
  dividerLine: { flex: 1, height: 1, backgroundColor: colors.border },
  dividerText: { fontSize: 13, color: colors.mutedForeground },
  socialBtn: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 12,
    paddingVertical: 14, borderRadius: 12,
    backgroundColor: colors.white, borderWidth: 1, borderColor: colors.border,
  },
  socialIcon: { fontSize: 18 },
  socialText: { fontSize: 14, color: colors.foreground },
  privacySection: { marginTop: 32 },
  privacyBox: {
    flexDirection: 'row', alignItems: 'flex-start', gap: 8,
    padding: 12, backgroundColor: colors.primaryLight, borderRadius: 10, marginBottom: 16,
  },
  privacyText: { fontSize: 12, color: colors.foreground, flex: 1 },
  signupRow: { flexDirection: 'row', justifyContent: 'center' },
  signupLabel: { fontSize: 13, color: colors.mutedForeground },
  signupLink: { fontSize: 13, color: colors.primary },
});