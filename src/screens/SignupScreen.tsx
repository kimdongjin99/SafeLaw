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
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Shield, Mail, Lock, User, Eye, EyeOff, Check } from 'lucide-react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { colors } from '../theme/colors';
import apiClient from '../api/client';

interface Props {
  onSignup?: () => void;
  navigation: any;
}

export function SignupScreen({ onSignup, navigation }: Props) {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
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
    formData.name.trim() !== '' &&
    formData.email.trim() !== '' &&
    formData.password.length >= 8 &&
    isPasswordMatch &&
    agreeToTerms &&
    agreeToPrivacy &&
    !isLoading;

  const handleSignup = async () => {
    if (!canSubmit) return;

    setIsLoading(true);
    try {
      // API 명세서 기준: POST /api/v1/auth/signup 호출 및 termsAgreed 전송
      const response = await apiClient.post('/api/v1/auth/signup', {
        name: formData.name.trim(),
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
        termsAgreed: agreeToTerms && agreeToPrivacy, // 필수 약관 동의 여부 (boolean)
      });

      // 가입 즉시 로그인 처리 (응답받은 accessToken 저장)
      if (response.data?.accessToken) {
        await EncryptedStorage.setItem('accessToken', response.data.accessToken);
      }

      Alert.alert('가입 완료', '회원가입이 성공적으로 완료되었습니다.', [
        { text: '확인', onPress: () => onSignup?.() }
      ]);
      
    } catch (error: any) {
      console.error('Signup Error:', error);
      
      const errorCode = error.response?.data?.code;
      const errorMsg = error.response?.data?.message || '회원가입 처리 중 문제가 발생했습니다.';

      if (errorCode === 'EMAIL_ALREADY_EXISTS' || error.response?.status === 409) {
        Alert.alert('가입 실패', '이미 가입된 이메일입니다.');
      } else {
        Alert.alert('가입 실패', errorMsg);
      }
    } finally {
      setIsLoading(false);
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
                  editable={!isLoading}
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
                  editable={!isLoading}
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
                  editable={!isLoading}
                />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)} disabled={isLoading}>
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
                  editable={!isLoading}
                />
                <TouchableOpacity onPress={() => setShowConfirm(!showConfirm)} disabled={isLoading}>
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
                disabled={isLoading}
              />
              <CheckboxItem
                checked={agreeToPrivacy}
                onChange={setAgreeToPrivacy}
                label="개인정보 처리방침에 동의합니다 (필수)"
                disabled={isLoading}
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
              onPress={handleSignup}
              disabled={!canSubmit}
            >
              {isLoading ? (
                <ActivityIndicator color={colors.white} />
              ) : (
                <Text style={styles.primaryBtnText}>회원가입</Text>
              )}
            </TouchableOpacity>

            {/* Login Link */}
            <View style={styles.loginRow}>
              <Text style={styles.loginLabel}>이미 계정이 있으신가요? </Text>
              <TouchableOpacity onPress={() => navigation.navigate('Login')} disabled={isLoading}>
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
  disabled,
}: {
  checked: boolean;
  onChange: (v: boolean) => void;
  label: string;
  disabled?: boolean;
}) {
  return (
    <TouchableOpacity 
      style={styles.checkRow} 
      onPress={() => !disabled && onChange(!checked)}
      activeOpacity={disabled ? 1 : 0.7}
    >
      <View style={[styles.checkbox, checked && styles.checkboxChecked, disabled && { opacity: 0.5 }]}>
        {checked && <Check color={colors.white} size={12} />}
      </View>
      <Text style={[styles.checkLabel, disabled && { opacity: 0.5 }]}>{label}</Text>
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