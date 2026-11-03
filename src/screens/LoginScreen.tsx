import React, { useState, useEffect } from 'react';
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
import { GoogleSignin, statusCodes } from '@react-native-google-signin/google-signin';
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

  useEffect(() => {
    // Google Sign-In 설정 초기화
    GoogleSignin.configure({
      webClientId: '114218259630-rl2kl1p4j0r68cdhvdlfma64q3f041nv.apps.googleusercontent.com',
    });
  }, []);

  const canSubmit =
    formData.email.trim() !== '' &&
    formData.password.trim() !== '' &&
    !isLoading;

  const handleLogin = async () => {
    if (!canSubmit) return;

    setIsLoading(true);
    try {
      // 이메일 로그인 API
      const response = await apiClient.post('/api/v1/auth/login', {
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
      });

      console.log('이메일 로그인 성공:', response.data);
      await handleLoginSuccess(response.data);
    } catch (error: any) {
      console.error('Email Login Error:', error);
      handleLoginError(error);
    } finally {
      setIsLoading(false);
    }
  };

  // 소셜 로그인 공통 처리 함수
  const processSocialLogin = async (
    provider: 'google' | 'apple',
    idToken: string,
    name?: string,
    termsAgreed?: boolean
  ) => {
    setIsLoading(true);
    try {
      const endpoint =
        provider === 'google' ? '/api/v1/auth/google' : '/api/v1/auth/apple';
      const payload: any = { idToken };
      if (name) payload.name = name;
      if (termsAgreed !== undefined) payload.termsAgreed = termsAgreed;

      const response = await apiClient.post(endpoint, payload);
      console.log(`${provider} 로그인 성공:`, response.data);
      await handleLoginSuccess(response.data);
    } catch (error: any) {
      console.error(`${provider} Login Error:`, error);
      const errorCode = error.response?.data?.code;

      if (error.response?.status === 400 && errorCode === 'TERMS_NOT_AGREED') {
        // 최초 가입자인 경우 약관 동의 팝업 표시
        Alert.alert(
          '약관 동의 필요',
          '처음 오셨군요! 서비스 이용을 위해 필수 약관(온디바이스 AI 처리 안내 포함)에 동의하시겠습니까?',
          [
            { text: '취소', style: 'cancel' },
            {
              text: '동의하고 시작하기',
              onPress: () => processSocialLogin(provider, idToken, name, true),
            },
          ]
        );
      } else {
        handleLoginError(error);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    // 1. 이미 로그인 진행 중인 경우 중복 호출 차단 (State Locking)
    if (isLoading) return;
    setIsLoading(true);

    try {
      // 2. Google Play 서비스 유효성 체크
      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });

      // 3. 테스트 및 계정 변경을 위해 매번 계정 선택 팝업이 뜨도록 기존 구글 세션 초기화
      try {
        await GoogleSignin.signOut();
      } catch (e) {
        // 이미 로그아웃된 상태인 경우 에러 무시
      }

      // 4. 구글 로그인 창 호출
      const response = await GoogleSignin.signIn();

      // SDK 버전별 토큰/유저명 추출 호환 처리
      const idToken = response.data?.idToken || (response as any).idToken;
      const name = response.data?.user?.name || (response as any).user?.name;

      if (!idToken) {
        Alert.alert('로그인 실패', 'Google 인증 토큰(idToken)을 받지 못했습니다.');
        return;
      }

      // 5. 백엔드로 ID 토큰 전송
      await processSocialLogin('google', idToken, name);
    } catch (error: any) {
      if (error.code === statusCodes.SIGN_IN_CANCELLED) {
        console.log('사용자가 구글 로그인을 취소했습니다.');
      } else if (error.code === statusCodes.IN_PROGRESS) {
        console.log('구글 로그인이 이미 진행 중입니다.');
      } else {
        console.error('Google Sign-In Error:', error);
        Alert.alert('로그인 오류', '구글 로그인 중 오류가 발생했습니다.');
      }
    } finally {
      // 6. 완료 또는 오류 처리 완료 후 항상 로딩 락 해제
      setIsLoading(false);
    }
  };

  const handleAppleLogin = async () => {
    Alert.alert('알림', 'Apple 로그인은 준비 중입니다.');
  };

  const handleLoginSuccess = async (data: any) => {
    if (data?.accessToken) {
      await EncryptedStorage.setItem('accessToken', data.accessToken);
      if (onLogin) onLogin();
      if (navigation) {
        navigation.reset({
          index: 0,
          routes: [{ name: 'Home' }],
        });
      }
    } else {
      Alert.alert('로그인 실패', '서버로부터 인증 토큰을 받지 못했습니다.');
    }
  };

  const handleLoginError = (error: any) => {
    const errorCode = error.response?.data?.code;
    const errorMsg = error.response?.data?.message || '로그인 중 오류가 발생했습니다.';

    if (errorCode === 'INVALID_CREDENTIALS' || error.response?.status === 401) {
      Alert.alert('로그인 실패', '이메일 또는 비밀번호가 올바르지 않거나 토큰이 만료되었습니다.');
    } else if (errorCode === 'TOO_MANY_LOGIN_ATTEMPTS' || error.response?.status === 429) {
      Alert.alert('로그인 제한', '로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.');
    } else if (errorCode === 'EMAIL_REGISTERED_WITH_OTHER_METHOD' || error.response?.status === 409) {
      Alert.alert('로그인 실패', '이미 다른 방법으로 가입된 이메일입니다. 기존 방법으로 로그인해 주세요.');
    } else if (errorCode === 'SOCIAL_LOGIN_NOT_CONFIGURED' || error.response?.status === 503) {
      Alert.alert('알림', '현재 소셜 로그인을 준비 중입니다. 잠시 후 다시 시도해 주세요.');
    } else {
      Alert.alert('로그인 실패', errorMsg);
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

            {/* Email Login Button */}
            <TouchableOpacity
              style={[styles.primaryBtn, !canSubmit && styles.disabledBtn]}
              onPress={handleLogin}
              disabled={!canSubmit}
            >
              {isLoading ? (
                <ActivityIndicator color={colors.white} />
              ) : (
                <Text style={styles.primaryBtnText}>이메일로 로그인</Text>
              )}
            </TouchableOpacity>

            {/* Divider */}
            <View style={styles.dividerRow}>
              <View style={styles.divider} />
              <Text style={styles.dividerText}>또는</Text>
              <View style={styles.divider} />
            </View>

            {/* Social Login Buttons */}
            <TouchableOpacity
              style={[styles.socialBtn, styles.googleBtn]}
              onPress={handleGoogleLogin}
              disabled={isLoading}
            >
              <Text style={styles.googleBtnText}>Google로 계속하기</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.socialBtn, styles.appleBtn]}
              onPress={handleAppleLogin}
              disabled={isLoading}
            >
              <Text style={styles.appleBtnText}>Apple로 계속하기</Text>
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
  dividerRow: { flexDirection: 'row', alignItems: 'center', marginVertical: 8 },
  divider: { flex: 1, height: 1, backgroundColor: colors.inputBackground },
  dividerText: { marginHorizontal: 10, fontSize: 13, color: colors.mutedForeground },
  socialBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    borderRadius: 12,
    borderWidth: 1,
  },
  googleBtn: { backgroundColor: colors.white, borderColor: '#ddd' },
  googleBtnText: { fontSize: 16, fontWeight: '600', color: colors.foreground },
  appleBtn: { backgroundColor: '#000', borderColor: '#000' },
  appleBtnText: { fontSize: 16, fontWeight: '600', color: colors.white },
  signupRow: { flexDirection: 'row', justifyContent: 'center', marginTop: 12 },
  signupLabel: { fontSize: 13, color: colors.mutedForeground },
  signupLink: { fontSize: 13, color: colors.primary, fontWeight: '600' },
});