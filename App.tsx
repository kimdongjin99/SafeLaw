import React, {useMemo, useState} from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  NativeModules,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {Cpu, Play, RotateCcw} from 'lucide-react-native';
import {
  SafeAreaProvider,
  SafeAreaView,
} from 'react-native-safe-area-context';

type RunState = 'idle' | 'loading' | 'ready' | 'generating' | 'error';

interface LlamaNativeModule {
  loadLocalModel(): Promise<string>;
  generate(prompt: string, maxTokens: number): Promise<string>;
  releaseModel(): Promise<void>;
}

const llamaModule = NativeModules.LlamaModule as LlamaNativeModule | undefined;
const TEST_MAX_TOKENS = 32;
const DEFAULT_QUESTION =
  '근로계약서를 작성하지 않은 경우 근로자는 어떻게 대응할 수 있나요?';

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function App() {
  const [runState, setRunState] = useState<RunState>('idle');
  const [modelPath, setModelPath] = useState('');
  const [question, setQuestion] = useState(DEFAULT_QUESTION);
  const [answer, setAnswer] = useState('');
  const [generationSeconds, setGenerationSeconds] = useState<number | null>(null);
  const [error, setError] = useState('');

  const isBusy = runState === 'loading' || runState === 'generating';
  const isReady = runState === 'ready' || runState === 'generating';
  const status = useMemo(() => {
    switch (runState) {
      case 'loading':
        return {label: '모델 불러오는 중', color: '#B46A16'};
      case 'ready':
        return {label: '모델 준비됨', color: '#16835A'};
      case 'generating':
        return {label: '답변 생성 중', color: '#2667A8'};
      case 'error':
        return {label: '오류 발생', color: '#B33A3A'};
      default:
        return {label: '모델 미로드', color: '#6B7280'};
    }
  }, [runState]);

  const requireModule = (): LlamaNativeModule => {
    if (!llamaModule) {
      throw new Error('LlamaModule이 네이티브 앱에 등록되지 않았습니다.');
    }
    return llamaModule;
  };

  const loadModel = async () => {
    setRunState('loading');
    setError('');
    try {
      const path = await requireModule().loadLocalModel();
      setModelPath(path);
      setRunState('ready');
    } catch (loadError) {
      setError(errorMessage(loadError));
      setRunState('error');
    }
  };

  const generateAnswer = async () => {
    const prompt = question.trim();
    if (!prompt) {
      setError('질문을 입력해 주세요.');
      return;
    }

    setRunState('generating');
    setAnswer('');
    setGenerationSeconds(null);
    setError('');
    const startedAt = Date.now();
    try {
      const result = await requireModule().generate(prompt, TEST_MAX_TOKENS);
      setAnswer(result.trim() || '모델이 빈 답변을 반환했습니다.');
      setGenerationSeconds((Date.now() - startedAt) / 1000);
      setRunState('ready');
    } catch (generateError) {
      setError(errorMessage(generateError));
      setRunState('error');
    }
  };

  const releaseModel = async () => {
    setError('');
    try {
      await requireModule().releaseModel();
      setRunState('idle');
      setModelPath('');
      setAnswer('');
      setGenerationSeconds(null);
    } catch (releaseError) {
      setError(errorMessage(releaseError));
      setRunState('error');
    }
  };

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
        <StatusBar barStyle="dark-content" backgroundColor="#F4F5F2" />
        <KeyboardAvoidingView
          style={styles.flex}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
          <ScrollView
            contentContainerStyle={styles.container}
            keyboardShouldPersistTaps="handled">
          <View style={styles.header}>
            <View style={styles.headerText}>
              <Text style={styles.eyebrow}>ON-DEVICE SLM TEST</Text>
              <Text style={styles.title}>SafeLaw 모델 테스트</Text>
            </View>
            <View style={styles.statusRow}>
              <View style={[styles.statusDot, {backgroundColor: status.color}]} />
              <Text style={styles.statusText}>{status.label}</Text>
            </View>
          </View>

          <View style={styles.modelPanel}>
            <View style={styles.modelInfo}>
              <Cpu color="#253238" size={22} strokeWidth={1.8} />
              <View style={styles.modelTextBlock}>
                <Text style={styles.modelName}>EXAONE SafeLaw V3 Q4_K_M</Text>
                <Text style={styles.modelPath} numberOfLines={2}>
                  {modelPath || '에뮬레이터의 로컬 GGUF 모델을 사용합니다.'}
                </Text>
              </View>
            </View>
            <View style={styles.modelActions}>
              <Pressable
                accessibilityRole="button"
                disabled={isBusy || isReady}
                onPress={loadModel}
                style={({pressed}) => [
                  styles.loadButton,
                  (isBusy || isReady) && styles.disabledButton,
                  pressed && styles.pressedButton,
                ]}>
                {runState === 'loading' ? (
                  <ActivityIndicator color="#FFFFFF" size="small" />
                ) : (
                  <Cpu color="#FFFFFF" size={18} />
                )}
                <Text style={styles.buttonText}>
                  {isReady ? '로드 완료' : '모델 로드'}
                </Text>
              </Pressable>
              <Pressable
                accessibilityLabel="모델 메모리 해제"
                accessibilityRole="button"
                disabled={!isReady || isBusy}
                onPress={releaseModel}
                style={({pressed}) => [
                  styles.iconButton,
                  (!isReady || isBusy) && styles.disabledIconButton,
                  pressed && styles.pressedButton,
                ]}>
                <RotateCcw color="#36454C" size={19} />
              </Pressable>
            </View>
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>질문</Text>
            <TextInput
              editable={!isBusy}
              multiline
              onChangeText={setQuestion}
              placeholder="법률 질문을 입력하세요."
              placeholderTextColor="#8A9296"
              style={styles.input}
              textAlignVertical="top"
              value={question}
            />
            <Pressable
              accessibilityRole="button"
              disabled={!isReady || isBusy || !question.trim()}
              onPress={generateAnswer}
              style={({pressed}) => [
                styles.generateButton,
                (!isReady || isBusy || !question.trim()) &&
                  styles.disabledButton,
                pressed && styles.pressedButton,
              ]}>
              {runState === 'generating' ? (
                <ActivityIndicator color="#FFFFFF" size="small" />
              ) : (
                <Play color="#FFFFFF" fill="#FFFFFF" size={17} />
              )}
              <Text style={styles.buttonText}>
                {runState === 'generating' ? '생성 중' : '답변 생성'}
              </Text>
            </Pressable>
          </View>

          <View style={styles.section}>
            <View style={styles.outputHeader}>
              <Text style={styles.label}>모델 출력</Text>
              <Text style={styles.outputMeta}>
                최대 {TEST_MAX_TOKENS}토큰
                {generationSeconds !== null
                  ? ` / ${generationSeconds.toFixed(1)}초`
                  : ''}
              </Text>
            </View>
            <View style={styles.outputPanel}>
              {runState === 'generating' ? (
                <View style={styles.outputLoading}>
                  <ActivityIndicator color="#2667A8" />
                  <Text style={styles.outputHint}>
                    로컬 CPU에서 생성하고 있습니다.
                  </Text>
                </View>
              ) : (
                <Text style={answer ? styles.answer : styles.outputHint}>
                  {answer ||
                    '모델을 로드한 뒤 질문을 실행하면 답변이 표시됩니다.'}
                </Text>
              )}
            </View>
          </View>

          {!!error && (
            <View style={styles.errorPanel}>
              <Text style={styles.errorTitle}>실행 오류</Text>
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  flex: {flex: 1},
  safeArea: {flex: 1, backgroundColor: '#F4F5F2'},
  container: {padding: 20, paddingBottom: 40, gap: 22},
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 16,
  },
  headerText: {flex: 1},
  eyebrow: {fontSize: 11, color: '#657078', fontWeight: '700'},
  title: {fontSize: 25, lineHeight: 32, color: '#172126', fontWeight: '700'},
  statusRow: {flexDirection: 'row', alignItems: 'center', gap: 7, paddingTop: 7},
  statusDot: {width: 8, height: 8, borderRadius: 4},
  statusText: {fontSize: 12, color: '#4D5960', fontWeight: '600'},
  modelPanel: {
    backgroundColor: '#FFFFFF',
    borderColor: '#D9DEDF',
    borderWidth: 1,
    borderRadius: 8,
    padding: 15,
    gap: 14,
  },
  modelInfo: {flexDirection: 'row', alignItems: 'flex-start', gap: 11},
  modelTextBlock: {flex: 1, gap: 4},
  modelName: {fontSize: 15, color: '#253238', fontWeight: '700'},
  modelPath: {fontSize: 12, lineHeight: 17, color: '#69757A'},
  modelActions: {flexDirection: 'row', gap: 8},
  loadButton: {
    minHeight: 44,
    flex: 1,
    borderRadius: 6,
    backgroundColor: '#176B50',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  buttonText: {fontSize: 14, color: '#FFFFFF', fontWeight: '700'},
  iconButton: {
    width: 44,
    height: 44,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#CAD0D2',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F7F8F6',
  },
  section: {gap: 9},
  label: {fontSize: 13, color: '#344249', fontWeight: '700'},
  input: {
    minHeight: 118,
    borderWidth: 1,
    borderColor: '#C8CFD1',
    borderRadius: 8,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    lineHeight: 22,
    color: '#1D292E',
  },
  generateButton: {
    height: 46,
    borderRadius: 6,
    backgroundColor: '#263F52',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  disabledButton: {backgroundColor: '#A5ADAF'},
  disabledIconButton: {opacity: 0.4},
  pressedButton: {opacity: 0.82},
  outputPanel: {
    minHeight: 180,
    borderWidth: 1,
    borderColor: '#D2D7D8',
    borderRadius: 8,
    backgroundColor: '#FAFBF9',
    padding: 15,
  },
  outputHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  outputMeta: {fontSize: 12, color: '#7A858A'},
  outputLoading: {flexDirection: 'row', alignItems: 'center', gap: 10},
  outputHint: {fontSize: 14, lineHeight: 21, color: '#7A858A'},
  answer: {fontSize: 15, lineHeight: 24, color: '#202C31'},
  errorPanel: {
    borderLeftWidth: 4,
    borderLeftColor: '#B33A3A',
    borderRadius: 4,
    backgroundColor: '#FFF2F0',
    padding: 13,
    gap: 4,
  },
  errorTitle: {fontSize: 13, color: '#932F2F', fontWeight: '700'},
  errorText: {fontSize: 13, lineHeight: 19, color: '#743434'},
});

export default App;
