import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Shield, Lock, Zap, MessageSquare, Download } from 'lucide-react-native';
import { colors } from '../theme/colors';
import apiClient from '../api/client';

interface Props {
  navigation: any;
}

export function HomeScreen({ navigation }: Props) {
  const [userName, setUserName] = useState<string>('');
  const [modelVersion, setModelVersion] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initializeHome = async () => {
      try {
        // 인터셉터에서 토큰을 자동 주입하므로 바로 엔드포인트만 호출합니다
        const userRes = await apiClient.get('/api/v1/users/me');
        setUserName(userRes.data.name);

        const modelRes = await apiClient.get('/api/v1/system/model/latest');
        setModelVersion(modelRes.data.version);
      } catch (error: any) {
        console.error('API 연동 에러:', error);
        if (error.response?.status === 401) {
          Alert.alert('알림', '로그인이 만료되었습니다. 다시 로그인해주세요.');
          navigation.replace('Login');
        }
      } finally {
        setIsLoading(false);
      }
    };

    initializeHome();
  }, [navigation]);

  if (isLoading) {
    return (
      <SafeAreaView style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color={colors.primary} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.header}>
          <View style={styles.logoBox}>
            <Shield color={colors.white} size={32} />
          </View>
          <Text style={styles.title}>
            {userName ? `${userName}님,\n환영합니다` : '법률 AI 어시스턴트'}
          </Text>
          <Text style={styles.subtitle}>
            개인정보 보호가 보장되는 온디바이스 AI 법률 상담
          </Text>
        </View>

        {modelVersion && (
          <View style={styles.modelStatusBox}>
            <Download color={colors.primary} size={16} />
            <Text style={styles.modelStatusText}>최신 AI 모델 준비 완료 (v{modelVersion})</Text>
          </View>
        )}

        <View style={styles.features}>
          <FeatureCard
            icon={<Lock color={colors.primary} size={20} />}
            title="완벽한 프라이버시"
            description="모든 데이터는 기기 내에서만 처리됩니다"
          />
          <FeatureCard
            icon={<Zap color={colors.primary} size={20} />}
            title="즉각적인 응답"
            description="온디바이스 AI로 빠르고 정확한 답변"
          />
          <FeatureCard
            icon={<MessageSquare color={colors.primary} size={20} />}
            title="맞춤형 상담"
            description="당신의 상황에 최적화된 법률 조언"
          />
        </View>

        <TouchableOpacity
          style={styles.ctaBtn}
          onPress={() => navigation.navigate('Chat')}
        >
          <Text style={styles.ctaBtnText}>상담 시작하기</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

function FeatureCard({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <View style={styles.card}>
      <View style={styles.cardIcon}>{icon}</View>
      <View style={{ flex: 1 }}>
        <Text style={styles.cardTitle}>{title}</Text>
        <Text style={styles.cardDesc}>{description}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  center: { justifyContent: 'center', alignItems: 'center' },
  scroll: { paddingHorizontal: 24, paddingBottom: 32 },
  header: { paddingTop: 40, paddingBottom: 24 },
  logoBox: {
    width: 64, height: 64, backgroundColor: colors.primary,
    borderRadius: 16, alignItems: 'center', justifyContent: 'center', marginBottom: 16,
  },
  title: { fontSize: 26, fontWeight: '700', color: colors.foreground, marginBottom: 8, lineHeight: 34 },
  subtitle: { fontSize: 14, color: colors.mutedForeground },
  modelStatusBox: {
    flexDirection: 'row', alignItems: 'center', gap: 8,
    backgroundColor: colors.primaryLight, padding: 12, borderRadius: 8,
    marginBottom: 24,
  },
  modelStatusText: { fontSize: 13, color: colors.primary, fontWeight: '500' },
  features: { gap: 12, marginBottom: 32 },
  card: {
    flexDirection: 'row', alignItems: 'flex-start', gap: 16,
    padding: 16, backgroundColor: colors.secondary, borderRadius: 12,
  },
  cardIcon: {
    width: 40, height: 40, backgroundColor: colors.white, borderRadius: 8,
    alignItems: 'center', justifyContent: 'center',
    borderWidth: 1, borderColor: colors.border,
  },
  cardTitle: { fontSize: 15, fontWeight: '600', color: colors.foreground, marginBottom: 4 },
  cardDesc: { fontSize: 13, color: colors.mutedForeground },
  ctaBtn: {
    backgroundColor: colors.primary, paddingVertical: 16,
    borderRadius: 12, alignItems: 'center',
  },
  ctaBtnText: { color: colors.white, fontSize: 16, fontWeight: '600' },
});