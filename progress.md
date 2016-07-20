### 需要翻译的类
顺序按照package出现的倒序来进行排列,每个子package再按照正序来进行排列

|   类名              |   描述                  |   状态  |
|--------             |:-------:               |:-----:  |
|   UnresolveablePropertyException |    不能解析变量异常    | 已完成   |
|   Unit                |   转换单元    |   已完成 |
|   ScriptRuntimeException  | 脚本运行期异常   |   已完成 |
|   PropertyAccessor    |   |   未完成 |
|   PropertyAccessException |   |   未完成 |
|   PreProcessor    | 编译期字符串预处理 |   已完成 |
|   ParserContext   |   解析上下文   | 未完成   |
|   ParserConfiguration |   解析配置    |   未完成 |
|   OptimizationFailure  | asm优化访问异常  | 已完成 |
|   Operator    | 操作符   |   已完成 |
|   MVELRuntime |   |   未完成 |
|   MVELInterpretedRuntime  |   |   未完成 |
|   MVEL   |    主运行类    |   已完成 |
|   MacroProcessor  |   |   未完成 |
|   Macro   |   宏定义 |   已完成 |
|   ImmutableElementException   |   不可变集合修改异常   |   已完成 |
|   ErrorDetail |   |   未完成 |
|   DataTypes   |   各种数据类型  |   已完成 |
|   DataConversion  |   数据转换工具类 |   已完成 |
|   ConversionHandler   |   转换处理器接口 |   已完成 |
|   ConversionException | 数据转换异常    |   已完成 |
|   CompileException    |   |   未完成 |
|   ArrayTools  |   |   未完成 |
|   ASTBinaryTree   |   |   未完成 |
|   ASTIterator |   |   未完成 |
|   ASTLinkedList   |   |   未完成 |
|   CallableProxy   |   调用代理对象  |   已完成 |
|   CollectionParser    | 集合解析器  |  未完成 |
|   CompatibilityStrategy   |   |   未完成 |
|   CompilerTools   | 编译工具类  |  未完成 |
|   ErrorUtil   |   |   未完成 |
|   ExecutionStack    |  一个简单的执行栈       |    已完成 |
|   FastList    |   |   未完成 |
|   FunctionParser  |   函数解析器 | 未完成 |
|   InternalNumber  |   |   未完成 |
|   JITClassLoader  |   |   未完成 |
|   LineMapper  |   行映射处理   |   未完成 |
|   Make    |   |   未完成 |
|   MethodStub  |   方法句柄    |   未完成 |
|   MVELClassLoader |   自定义类加载器 |   已完成 |
|   NullType    |   空值类型    |   已完成 |
|   ParseTools  |   解析工具类   |   已完成 |
|   PropertyTools   |   属性工具类   |   未完成 |
|   ProtoParser |   |   未完成 |
|   ReflectionUtil  |   |   未完成 |
|   SharedVariableSpaceModel    |   |   未完成 |
|   SimpleIndexHashMapWrapper   |   |   未完成 |
|   SimpleVariableSpaceModel    |   |   未完成 |
|   Soundex |   |   未完成 |
|   Stack   | 栈接口(此接口无用)    |   不作处理 |
|   StackElement    |   栈节点 |   已完成 |
|   StaticFieldStub |   静态字段句柄(未使用) |   不作处理    |
|   StaticStub  |   |   未完成 |
|   StringAppender  |   |   未完成 |
|   ThisLiteral |   (未使用)   |   不作处理    |
|   Varargs |   可变参数解析器 |   未完成 |
|   VariableSpaceCompiler   |   |   未完成 |
|   VariableSpaceModel  |   |   未完成 |
|   DynamicAccessor |   动态访问器   |   已完成 |
|   DynamicClassLoader  |   动态类访问加载器    |   未完成 |
|   DynamicCollectionAccessor   |   |   未完成 |
|   DynamicGetAccessor  |   get动态访问器    |   未完成 |
|   DynamicOptimizer    |   动态访问优化器 |   未完成 |
|   DynamicSetAccessor  |   set动态访问器    |   未完成 |
|   ASMAccessorOptimizer    |   |   未完成 |
|   ProducesBytecode    |   |   未完成 |
|   ArrayCreator    |   |   未完成 |
|   ExprValueAccessor   |   |   未完成 |
|   ListCreator |   |   未完成 |
|   MapCreator  |   |   未完成 |
|   MDArrayCreator  |   (未使用)   |   不作处理    |
|   ArrayAccessor   |   数组访问器   |   未完成 |
|   ArrayAccessorNest   |   数组访问器(下标需要计算)   |   未完成 |
|   ArrayLength |   数组长度访问器 |   未完成 |
|   BaseAccessor    |   |   未完成 |
|   ConstructorAccessor |   构建函数访问器 |   未完成 |
|   DynamicFieldAccessor    |   字段访问器   |   未完成 |
|   DynamicFunctionAccessor |   函数访问器   |   未完成 |
|   DynamicSetterAccessor   |   (未使用)   |   不作处理 |
|   FieldAccessor   |   字段访问器   |   未完成 |
|   FieldAccessorNH |   字段访问器(带null处理)  |   未完成 |
|   FunctionAccessor    |   函数访问器   |   未完成    |
|   GetterAccessor  |   getter方法访问器 |   未完成 |
|   GetterAccessorNH    |   getter方法访问器(带null处理)    |   未完成 |
|   IndexedCharSeqAccessor  |   字符串下标访问器    |   未完成 |
|   IndexedCharSeqAccessorNest  |   字符串下标访问器(计算下标)  |   未完成 |
|   IndexedVariableAccessor |   下标变量访问器 |   未完成 |
|   InvokableAccessor   |   抽象调用访问器 |   未完成 |
|   ListAccessor    |   List集合访问器   |   未完成 |
|   ListAccessorNest    |   List集合访问器(计算下标) |   未完成 |
|   MapAccessor |   Map访问器  |   未完成 |
|   MapAccessorNest |   Map访问器(计算属性)    |   未完成 |
|   MethodAccessor  |   方法调用访问器 |   未完成 |
|   MethodAccessorNH    |   方法调用访问器(带null处理)    |   未完成 |
|   Notify  |   通知监听器   |   未完成 |
|   NullSafe    |   null安全访问器   |   未完成 |
|   PropertyHandlerAccessor |   属性访问器   |   未完成 |
|   SetterAccessor  |   set方法访问器    |   未完成 |
|   StaticReferenceAccessor |   静态对象引用访问器   |   未完成 |
|   StaticVarAccessor   |   静态字段访问器 |   未完成 |
|   StaticVarAccessorNH |   静态字段访问器(带null处理)    |   未完成 |
|   ThisValueAccessor   |   this引用访问器   |   未完成 |
|   Union   |   |   未完成 |
|   VariableAccessor    |   变量信息访问器 |   未完成 |
|   WithAccessor    |   |   未完成 |
|   ReflectiveAccessorOptimizer |   反射访问优化器 |   未完成 |
|   AbstractOptimizer   |  抽象优化器    |   未完成 |
|   AccessorOptimizer   |   访问优化器接口定义   |   未完成 |
|   OptimizationNotSupported    |   优化不受支持异常    |   已完成 |
|   OptimizerFactory    |   优化器工厂   |   已完成 |
|   OptimizerHook   |   (未使用)   |   不作处理    |
|   MathProcessor   |   数学运算处理器 |   未完成 |
|   BaseVariableResolverFactory |   解析器工厂基础类    |   未完成 |
|   CachedMapVariableResolverFactory    |   实时缓存map解析器工厂  |   未完成 |
|   CachingMapVariableResolverFactory   |   指定缓存map解析器工厂    |   未完成 |
|   ClassImportResolverFactory  |   类引用解析器工厂    |   未完成 |
|   DefaultLocalVariableResolverFactory |   本地作用域解析器工厂  |   未完成 |
|   FunctionVariableResolverFactory |   函数体内解析器工厂   |   未完成 |
|   ImmutableDefaultFactory |   不可变解析器工厂    |   未完成|
|   IndexedVariableResolverFactory  |   下标解析器工厂 |   未完成 |
|   IndexVariableResolver   |   下标解析器   |   未完成 |
|   ItemResolverFactory |   单值解析器工厂 |   未完成 |
|   LocalVariableResolverFactory    |   本地作用域解析器工厂标记接口  |   已完成 |
|   MapVariableResolver |   外部map解析器  |   未完成 |
|   MapVariableResolverFactory  |   外部map解析器工厂  |   未完成 |
|   PrecachedMapVariableResolver    |   map占位entity解析器  |   未完成 |
|   SimpleSTValueResolver   |   声明值解析器  |   未完成 |
|   SimpleValueResolver |   单值解析器   |   未完成 |
|   SimpleVariableResolverFactory   |   (未使用)   |   不作处理    |
|   StackDelimiterResolverFactory   |   委托解析处理工厂    |   未完成 |
|   StackDemarcResolverFactory  |   委托解析但不中断委托类解析器工厂    |   未完成 |
|   StackResetResolverFactory   |   栈式可重用解析器工厂  |   未完成 |
|   StaticMethodImportResolver  |   静态方法引用解析器(未使用)   |   不作处理 |
|   StaticMethodImportResolverFactory   |   (未使用)   |   不作处理    |
|   TypeInjectionResolverFactory    |   (未使用)   |   不作处理    |
|   TypeInjectionResolverFactoryImpl    |   (未使用)   |   不作处理    |
|   GlobalListenerFactory   |   全局监听器工厂   |   未完成 |
|   Interceptor |   拦截器接口 |   未完成 |
|   Listener    |   调用监听器接口   |   未完成 |
|   PropertyHandler |   属性处理器接口 |   未完成 |
|   PropertyHandlerFactory  |   属性处理器工厂 |   未完成 |
|   ResolverTools   |   解析器工厂工具类    |   未完成 |
|   VariableResolver    |   变量解析器接口 |   未完成 |
|   VariableResolverFactory |   解析器工厂接口 |   未完成 |
|   Debugger    |   |   未完成 |
|   DebuggerContext |   |   未完成 |
|   DebugTools  |   |   未完成 |
|   Frame   |   |   未完成 |
|   ArrayCH |   (未使用)   |   不作处理    |
|   ArrayHandler    |   数组转换器   |   未完成 |
|   BigDecimalCH    |   数据转bigDecimal转换器    |   未完成 |
|   BigIntegerCH    |   数据转bigInteger转换器    |   未完成 |
|   BooleanCH   |   数据转boolean转换器   |   未完成 |
|   ByteCH  |   数据转byte转换器  |   未完成 |
|   CharArrayCH |   字符串转字符数组转换器 |   未完成 |
|   CharCH  |   数据转char转换器  |   未完成 |
|   CompositeCH |   组合转换器   |   未完成 |
|   Converter   |   转换函数接口  |   已完成 |
|   DoubleCH    |   数据转double转换器    |   未完成 |
|   FloatCH |   数据转float转换器 |   未完成 |
|   IntArrayCH  |   数据转int[]转换器 |   未完成 |
|   IntegerCH   |   数据转int转换器   |   未完成 |
|   ListCH  |   数据转list转换器  |   未完成 |
|   LongCH  |   数据转long转换器  |   未完成 |
|   ObjectCH    |   数据转object转换器    |   未完成 |
|   PrimIntArrayCH  |   转基本类型int[]转换器(未使用)  |   不作处理    |
|   SetCH   |   数据转set转换器   |   未完成 |
|   ShortCH |   数据转short转换器 |   未完成 |
|   StringArrayCH   |   对象转string[]转换器  |   未完成 |
|   StringCH    |   对象转string转换器    |   未完成 |
|   UnitConversion  |   (未使用)   |   不作处理    |
|   AbstractParser  |   抽象解析器   |   未完成 |
|   Accessor    |   属性访问器接口 |   未完成 |
|   AccessorNode    |   节点访问器接口 |   未完成 |
|   BlankLiteral    |   空值常量定义  |   未完成 |
|   CompiledAccExpression   |   已编译访问器表达式   |   未完成 |
|   CompiledExpression  |   编译表达式   |   未完成 |
|   EndWithValue    |   (未使用)   |   不作处理    |
|   ExecutableAccessor  |   可执行的节点访问器   |   未完成 |
|   ExecutableAccessorSafe  |   |   未完成 |
|   ExecutableLiteral   |   常量计算单元  |   未完成 |
|   ExecutableStatement |   可执行节点接口 |   未完成 |
|   ExpressionCompiler  |   表达式编译器    |   未完成 |
|   Parser  |   编译器接口定义 |   未完成 |
|   PropertyVerifier    |   属性验证器   |   未完成 |
|   And |   &&节点    |   已完成 |
|   ArraySize   |   数组长度表示  |   已完成 |
|   AssertNode  |   断言节点    |   未完成 |
|   Assignment  |   赋值节点接口  |   未完成 |
|   AssignmentNode  |   赋值节点    |   未完成 |
|   ASTNode |   抽象语法树节点统一类  |   未完成 |
|   BinaryOperation |   一元操作定义  |   未完成 |
|   BlockNode   |   语法块节点   |   未完成 |
|   BooleanNode |   二元操作定义  |   未完成 |
|   Contains    |   contains包含节点    |   未完成 |
|   Convertable |    convert转换节点    |   未完成 |
|   DeclProtoVarNode    |   原型变量声明节点    |   未完成 |
|   DeclTypedVarNode    |   变量声明节点  |   未完成 |
|   DeepAssignmentNode  |   深度赋值节点  |   未完成 |
|   DoNode  |   do循环节点  |   未完成 |
|   DoUntilNode |   do until语法块节点   |   未完成 |
|   EndOfStatement  |   语句结束标记节点    |   未完成 |
|   Fold    |   fold表达式节点   |   未完成 |
|   ForEachNode |   增强for循环节点   |   未完成 |
|   ForNode |   for循环节点 |   未完成 |
|   Function    |   函数定义节点  |   未完成 |
|   FunctionInstance    |   函数实例节点  |   未完成 |
|   IfNode  |   if节点    |   未完成 |
|   ImportNode  |   import引入节点  |   未完成 |
|   IndexedAssignmentNode   |   已解析变量赋值节点   |   未完成 |
|   IndexedDeclTypedVarNode |   指定变量位置声明节点  |   未完成 |
|   IndexedOperativeAssign  |   已解析变量操作赋值节点 |   未完成 |
|   IndexedPostFixDecNode   |   已解析变量后置--节点 |   未完成 |
|   IndexedPostFixIncNode   |   已解析变量后置++节点 |   未完成 |
|   IndexedPreFixDecNode    |   已解析变量前置--节点 |   未完成 |
|   IndexedPreFixIncNode    |   已解析变量前置++节点 |   未完成 |
|   InlineCollectionNode    |   内部集合表达式节点   |   未完成 |
|   Instance    |   instanceOf节点    |   未完成 |
|   IntAdd  |   整数相加节点  |   未完成 |
|   IntDiv  |   整数相除节点  |   未完成 |
|   InterceptorWrapper  |   拦截器包装节点 |   未完成 |
|   IntMult |   整数相乘节点  |   未完成 |
|   IntOptimized    |   整数处理优化节点接口  |   已完成 |
|   IntSub  |   整数相减节点  |   未完成 |
|   Invert  |   数字取反节点  |   未完成 |
|   InvokationContextFactory    |   内部执行上下文解析器工厂    |   未完成 |
|   IsDef   |   isDef探测节点   |   未完成 |
|   LineLabel   |   调度代码标记行节点   |   未完成 |
|   LiteralDeepPropertyNode |   多级常量节点  |   未完成 |
|   LiteralNode |   常量标识节点  |   未完成 |
|   Negation    |   boolean取反节点 |   未完成 |
|   NestedStatement |   |   未完成 |
|   NewObjectNode   |   new对象节点 |   未完成 |
|   NewObjectPrototype  |   new函数对象节点   |   未完成 |
|   NewPrototypeNode    |   new原型节点 |   未完成 |
|   OperativeAssign |   变量操作赋值节点    |   未完成 |
|   OperatorNode    |   操作符节点   |   未完成 |
|   Or  |   or操作节点  |   已完成 |
|   PostFixDecNode  |   后置--节点  |   已完成 |
|   PostFixIncNode  |   后置++节点  |   已完成 |
|   PreFixDecNode   |   前置--节点  |   已完成 |
|   PreFixIncNode   |   前置++节点  |   已完成 |
|   Proto   |   原型结构节点  |   未完成 |
|   PrototypalFunctionInstance  |   原型函数调用实例    |   未完成 |
|   ProtoVarNode    |   原型属性声明节点    |   未完成 |
|   ReduceableCodeException |   (未使用)   |   不作处理    |
|   RedundantCodeException  |   无限循环异常  |   已完成 |
|   RegExMatch  |   正则匹配表达式节点   |   未完成 |
|   RegExMatchNode  |   字符串正则匹配节点   |   未完成 |
|   ReturnNode  |   return节点    |   未完成 |
|   Safe    |   |   未完成 |
|   Sign    |   表达式取负节点 |   未完成 |
|   Soundslike  |   |   未完成 |
|   Stacklang   |   堆栈指令集节点 |   未完成 |
|   StaticImportNode    |   静态引用方法节点    |   未完成 |
|   Strsim  | 字符串匹配度节点  |   未完成 |
|   Substatement    |   子程序节点   |   未完成 |
|   ThisWithNode    |   with当前对象节点  |   未完成 |
|   TypeCast    |   类型转换节点  |   未完成 |
|   TypeDescriptor  |   类型声明节点  |   未完成 |
|   TypedVarNode    |   指定类型声明节点    |   未完成 |
|   Union   |   联合节点    |   已完成 |
|   UntilNode   |   until节点(与while相对)   |   已完成 |
|   WhileNode   |   while循环节点   |   已完成 |
|   WithNode    |   with节点  |   未完成 |