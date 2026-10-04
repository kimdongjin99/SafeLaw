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
import { Shield, Mail, Lock, Eye, EyeOff } from 'lucide-react-native';
import EncryptedStorage from 'react-native-encrypted-storage';
import { colors } from '../theme/colors';
import apiClient from '../api/client';

interface Props {
  onLogin?: () => void;
  navigation: any;
}

export function LoginScreen({ onLogin, navigation }: Props) {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });

  const canSubmit =
    formData.email.trim() !== '' &&
    formData.password.trim() !== '' &&
    !isLoading;

  const handleLogin = async () => {
    if (!canSubmit) return;

    setIsLoading(true);
    try {
      // API 명세서 기준: POST /api/v1/auth/login
      const response = await apiClient.post('/api/v1/auth/login', {
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
      });

      console.log('로그인 성공:', response.data);

      // 로그인 성공 시 accessToken 기기 저장
      if (response.data?.accessToken) {
        await EncryptedStorage.setItem('accessToken', response.data.accessToken);

        // 1. App.tsx 수준의 로그인 상태 업데이트 콜백이 있다면 실행
        if (onLogin) {
          onLogin();
        }

        // 2. 회원가입 화면으로 뒤로가기 되지 않도록 스택을 비우고 메인 화면으로 이동
        if (navigation) {
          navigation.reset({
            index: 0,
            routes: [{ name: 'Home' }], // 프로젝트의 메인 화면 라우트 이름이 'Home'이면 'Home'으로 변경
          });
        }
      } else {
        Alert.alert('로그인 실패', '서버로부터 인증 토큰을 받지 못했습니다.');
      }
    } catch (error: any) {
      console.error('Login Error:', error);

      const errorCode = error.response?.data?.code;
      const errorMsg = error.response?.data?.message || '로그인 중 오류가 발생했습니다.';

      if (errorCode === 'INVALID_CREDENTIALS' || error.response?.status === 401) {
        Alert.alert('로그인 실패', '이메일 또는 비밀번호가 올바르지 않습니다.');
      } else if (errorCode === 'TOO_MANY_LOGIN_ATTEMPTS' || error.response?.status === 429) {
        Alert.alert('로그인 제한', '로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.');
      } else if (error.response?.status === 404) {
        Alert.alert('로그인 실패', '로그인 API 경로를 찾을 수 없습니다.');
      } else {
        Alert.alert('로그인 실패', errorMsg);
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
              <Shield color={colors.white} size={36} />
            </View>
            <Text style={styles.title}>SafeLaw</Text>
            <Text style={styles.subtitle}>온디바이스 AI 법률 상담 서비스</Text>
          </View>

          {/* Form */}
          <View style={styles.form}>
            {/* Email Input */}
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

            {/* Password Input */}
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>비밀번호</Text>
              <View style={styles.inputRow}>
                <Lock color={colors.mutedForeground} size={20} />
                <TextInput
                  style={styles.input}
                  value={formData.password}
                  onChangeText={(v) => setFormData({ ...formData, password: v })}
                  placeholder="비밀번호 입력"
                  placeholderTextColor={colors.mutedForeground}
                  secureTextEntry={!showPassword}
                  editable={!isLoading}
                />
                <TouchableOpacity
                  onPress={() => setShowPassword(!showPassword)}
                  disabled={isLoading}
                >
                  {showPassword ? (
                    <EyeOff color={colors.mutedForeground} size={20} />
                  ) : (
                    <Eye color={colors.mutedForeground} size={20} />
                  )}
                </TouchableOpacity>
              </View>
            </View>

            {/* Login Button */}
            <TouchableOpacity
              style={[styles.primaryBtn, !canSubmit && styles.disabledBtn]}
              onPress={handleLogin}
              disabled={!canSubmit}
            >
              {isLoading ? (
                <ActivityIndicator color={colors.white} />
              ) : (
                <Text style={styles.primaryBtnText}>로그인</Text>
              )}
            </TouchableOpacity>

            {/* Signup Link */}
            <View style={styles.signupRow}>
              <Text style={styles.signupLabel}>계정이 없으신가요? </Text>
              <TouchableOpacity
                onPress={() => navigation?.navigate('Signup')}
                disabled={isLoading}
              >
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
  scroll: { paddingHorizontal: 24, paddingBottom: 40 },
  header: { alignItems: 'center', paddingTop: 60, paddingBottom: 32 },
  logoBox: {
    width: 72,
    height: 72,
    backgroundColor: colors.primary,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  title: { fontSize: 28, fontWeight: '700', color: colors.foreground, marginBottom: 6 },
  subtitle: { fontSize: 14, color: colors.mutedForeground },
  form: { gap: 18 },
  fieldGroup: { gap: 6 },
  label: { fontSize: 14, color: colors.foreground, paddingHorizontal: 4 },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    backgroundColor: colors.inputBackground,
    borderRadius: 12,
  },
  input: { flex: 1, fontSize: 14, color: colors.foreground },
  primaryBtn: {
    backgroundColor: colors.primary,
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 10,
  },
  disabledBtn: { opacity: 0.5 },
  primaryBtnText: { color: colors.white, fontSize: 16, fontWeight: '600' },
  signupRow: { flexDirection: 'row', justifyContent: 'center', marginTop: 12 },
  signupLabel: { fontSize: 13, color: colors.mutedForeground },
  signupLink: { fontSize: 13, color: colors.primary, fontWeight: '600' },
});