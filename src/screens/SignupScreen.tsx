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
import { Shield, Mail, Lock, User, Eye, EyeOff, Check } from 'lucide-react-native';
import { colors } from '../theme/colors';

interface Props {
  onSignup: () => void;
  navigation: any;
}

export function SignupScreen({ onSignup, navigation }: Props) {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [agreeToTerms, setAgreeToTerms] = useState(false);
  const [agreeToPrivacy, setAgreeToPrivacy] = useState(false);

  const isPasswordMatch = formData.password === formData.confirmPassword;
  const canSubmit =
    formData.name &&
    formData.email &&
    formData.password &&
    formData.confirmPassword &&
    isPasswordMatch &&
    agreeToTerms &&
    agreeToPrivacy;

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
              <Shield color={colors.white} size={32} />
            </View>
            <Text style={styles.title}>회원가입</Text>
            <Text style={styles.subtitle}>개인정보 보호가 보장되는 법률 상담</Text>
          </View>

          <View style={styles.form}>
            {/* Name */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>이름</Text>
              <View style={styles.inputRow}>
                <User color={colors.mutedForeground} size={20} />
                <TextInput
                  style={styles.input}
                  value={formData.name}
                  onChangeText={(v) => setFormData({ ...formData, name: v })}
                  placeholder="홍길동"
                  placeholderTextColor={colors.mutedForeground}
                />
              </View>
            </View>

            {/* Email */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>이메일</Text>
              <View style={styles.inputRow}>
                <Mail color={colors.mutedForeground} size={20} />
                <TextInput
                  style={styles.input}
                  value={formData.email}
                  onChangeText={(v) => setFormData({ ...formData, email: v })}
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
              <View style={styles.inputRow}>
                <Lock color={colors.mutedForeground} size={20} />
                <TextInput
                  style={styles.input}
                  value={formData.password}
                  onChangeText={(v) => setFormData({ ...formData, password: v })}
                  placeholder="8자 이상 입력하세요"
                  placeholderTextColor={colors.mutedForeground}
                  secureTextEntry={!showPassword}
                />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)}>
                  {showPassword
                    ? <EyeOff color={colors.mutedForeground} size={20} />
                    : <Eye color={colors.mutedForeground} size={20} />}
                </TouchableOpacity>
              </View>
              {formData.password.length > 0 && formData.password.length < 8 && (
                <Text style={styles.errorText}>비밀번호는 8자 이상이어야 합니다</Text>
              )}
            </View>

            {/* Confirm Password */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>비밀번호 확인</Text>
              <View style={styles.inputRow}>
                <Lock color={colors.mutedForeground} size={20} />
                <TextInput
                  style={styles.input}
                  value={formData.confirmPassword}
                  onChangeText={(v) => setFormData({ ...formData, confirmPassword: v })}
                  placeholder="비밀번호를 다시 입력하세요"
                  placeholderTextColor={colors.mutedForeground}
                  secureTextEntry={!showConfirm}
                />
                <TouchableOpacity onPress={() => setShowConfirm(!showConfirm)}>
                  {showConfirm
                    ? <EyeOff color={colors.mutedForeground} size={20} />
                    : <Eye color={colors.mutedForeground} size={20} />}
                </TouchableOpacity>
              </View>
              {formData.confirmPassword.length > 0 && !isPasswordMatch && (
                <Text style={styles.errorText}>비밀번호가 일치하지 않습니다</Text>
              )}
            </View>

            {/* Terms */}
            <View style={styles.termsGroup}>
              <CheckboxItem
                checked={agreeToTerms}
                onChange={setAgreeToTerms}
                label="서비스 이용약관에 동의합니다 (필수)"
              />
              <CheckboxItem
                checked={agreeToPrivacy}
                onChange={setAgreeToPrivacy}
                label="개인정보 처리방침에 동의합니다 (필수)"
              />
            </View>

            {/* Privacy Notice */}
            <View style={styles.privacyBox}>
              <Shield color={colors.primary} size={16} />
              <View style={{ flex: 1 }}>
                <Text style={styles.privacyTitle}>온디바이스 AI 처리 방식</Text>
                <Text style={styles.privacyDesc}>
                  모든 상담 내용과 개인정보는 기기 내에서만 처리되며, 외부 서버로 전송되지 않습니다
                </Text>
              </View>
            </View>

            {/* Signup Button */}
            <TouchableOpacity
              style={[styles.primaryBtn, !canSubmit && styles.disabledBtn]}
              onPress={onSignup}
              disabled={!canSubmit}
            >
              <Text style={styles.primaryBtnText}>회원가입</Text>
            </TouchableOpacity>

            {/* Login Link */}
            <View style={styles.loginRow}>
              <Text style={styles.loginLabel}>이미 계정이 있으신가요? </Text>
              <TouchableOpacity onPress={() => navigation.navigate('Login')}>
                <Text style={styles.loginLink}>로그인</Text>
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function CheckboxItem({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: (v: boolean) => void;
  label: string;
}) {
  return (
    <TouchableOpacity style={styles.checkRow} onPress={() => onChange(!checked)}>
      <View style={[styles.checkbox, checked && styles.checkboxChecked]}>
        {checked && <Check color={colors.white} size={12} />}
      </View>
      <Text style={styles.checkLabel}>{label}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  scroll: { paddingHorizontal: 24, paddingBottom: 40 },
  header: { alignItems: 'center', paddingTop: 48, paddingBottom: 24 },
  logoBox: {
    width: 64, height: 64, backgroundColor: colors.primary,
    borderRadius: 16, alignItems: 'center', justifyContent: 'center', marginBottom: 16,
  },
  title: { fontSize: 28, fontWeight: '700', color: colors.foreground, marginBottom: 8 },
  subtitle: { fontSize: 14, color: colors.mutedForeground },
  form: { gap: 16 },
  fieldGroup: { gap: 6 },
  label: { fontSize: 14, color: colors.foreground, paddingHorizontal: 4 },
  inputRow: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    paddingHorizontal: 16, paddingVertical: 14,
    backgroundColor: colors.inputBackground, borderRadius: 12,
  },
  input: { flex: 1, fontSize: 14, color: colors.foreground },
  errorText: { fontSize: 12, color: colors.destructive, paddingHorizontal: 4 },
  termsGroup: { gap: 12, paddingVertical: 4 },
  checkRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  checkbox: {
    width: 20, height: 20, borderRadius: 4,
    borderWidth: 2, borderColor: colors.border,
    alignItems: 'center', justifyContent: 'center',
    backgroundColor: colors.white,
  },
  checkboxChecked: { backgroundColor: colors.primary, borderColor: colors.primary },
  checkLabel: { fontSize: 14, color: colors.foreground, flex: 1 },
  privacyBox: {
    flexDirection: 'row', alignItems: 'flex-start', gap: 8,
    padding: 12, backgroundColor: colors.primaryLight, borderRadius: 10,
  },
  privacyTitle: { fontSize: 12, fontWeight: '600', color: colors.foreground, marginBottom: 4 },
  privacyDesc: { fontSize: 12, color: colors.mutedForeground },
  primaryBtn: {
    backgroundColor: colors.primary, paddingVertical: 16,
    borderRadius: 12, alignItems: 'center', marginTop: 8,
  },
  disabledBtn: { opacity: 0.5 },
  primaryBtnText: { color: colors.white, fontSize: 16, fontWeight: '600' },
  loginRow: { flexDirection: 'row', justifyContent: 'center', marginTop: 8 },
  loginLabel: { fontSize: 13, color: colors.mutedForeground },
  loginLink: { fontSize: 13, color: colors.primary },
});