import 'react-native-gesture-handler';

import React, { useEffect, useState } from 'react';
import { ActivityIndicator, View, StyleSheet } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack'; // 👈 native-stack 대신 안전한 stack으로 변경
import EncryptedStorage from 'react-native-encrypted-storage';

import { colors } from '../theme/colors';

// 화면 컴포넌트 불러오기
import { LoginScreen } from '../screens/LoginScreen';
import { SignupScreen } from '../screens/SignupScreen';
import { HomeScreen } from '../screens/HomeScreen';
import { ChatScreen } from '../screens/ChatScreen';
import { HistoryScreen } from '../screens/HistoryScreen';
import { SettingsScreen } from '../screens/SettingsScreen';

const Stack = createStackNavigator();

export function AppNavigator() {
  const [isLoading, setIsLoading] = useState(true);
  const [initialRoute, setInitialRoute] = useState<'Login' | 'Home'>('Login');

  useEffect(() => {
    const checkLoginStatus = async () => {
      try {
        const token = await EncryptedStorage.getItem('accessToken');
        if (token && token !== 'null' && token.trim() !== '') {
          setInitialRoute('Home');
        } else {
          setInitialRoute('Login');
        }
      } catch (error) {
        console.error('보안 토큰 확인 실패:', error);
        setInitialRoute('Login');
      } finally {
        setIsLoading(false);
      }
    };

    checkLoginStatus();
  }, []);

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={colors?.primary || '#007AFF'} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName={initialRoute}
        screenOptions={{ headerShown: false }}
      >
        <Stack.Screen name="Login" component={LoginScreen as any} />
        <Stack.Screen name="Signup" component={SignupScreen as any} />
        <Stack.Screen name="Home" component={HomeScreen as any} />
        <Stack.Screen name="Chat" component={ChatScreen as any} />
        <Stack.Screen name="History" component={HistoryScreen as any} />
        <Stack.Screen name="Settings" component={SettingsScreen as any} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors?.white || '#FFFFFF',
  },
});