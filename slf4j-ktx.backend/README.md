# slf4j-ktx.backend

**IR 변환 계층** — K1과 K2 양쪽 frontend가 생성한 선언의 IR을 받아 Companion.log property의 초기화식(`LoggerFactory.getLogger(FQN)`)과 각 level 함수의 body(`if (log.isXxxEnabled) log.xxx(message())`)를 채운다.

## 존재 이유

K1/K2 frontend는 **선언만** 만들고 body는 만들지 않는다 (descriptor/FIR 모두). body는 IR 단계에서 채워야 한다. K1과 K2의 IR은 동일한 `IrClass`/`IrProperty`/`IrSimpleFunction` 모델로 수렴하므로 **backend는 프론트엔드 중립적**이다.

다만 plugin-generated declaration을 IR에서 식별하려면 K1/K2 origin을 모두 체크해야 하고(`isFromPlugin(afterK2: Boolean)` 관용구), backend가 양쪽 모듈을 `implementation`으로 가진다 (`PluginKey` 참조 공유 목적).

## 주요 타입 (Step 7에서 구현)

- `Slf4jKtxLoweringExtension` — `IrGenerationExtension.generate()` 진입점. 파일별 transformer dispatch
- `Slf4jKtxPluginContext` — `IrPluginContext` 서브클래스. `afterK2: Boolean` + 외부 심볼 lazy 캐시 (`Logger`, `LoggerFactory.getLogger`, `Function0.invoke`)
- `Slf4jKtxIrGenerator` — `IrElementVisitorVoid`. `visitProperty` (backing field 확보 + initializer + getter body), `visitSimpleFunction` (level body)
- `Slf4jKtxIrPredicates` — `IrDeclaration.isFromPlugin(afterK2)` 확장

## 의존

- `compileOnly`: `kotlin-compiler-embeddable`
- `implementation`: `:slf4j-ktx.common`, `:slf4j-ktx.k1`, `:slf4j-ktx.k2`

## 참조

- SPEC §4.1, §5.5, §6.5
- serialization 대응: `kotlinx-serialization.backend/src/.../extensions/SerializationLoweringExtension.kt`, `kotlinx-serialization.backend/src/.../ir/IrPredicates.kt:204-211` (`isFromPlugin(afterK2)` 시그니처), `SerializerIrGenerator.kt`
