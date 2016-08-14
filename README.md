## 目的
用于翻译mvel的源码，进行源码性注释，以方便进行阅读。同时了解其内部的一些实现以及处理过程

## 注意事项
1. 此注释过程将保证不会对原来的代码进行改动，并且相应的注释均是在代码的上方使用//或者是添加到原有的代码当中，使用新增行的方式。
2. 每一版注释将与特定的版本相对应

## 翻译进度表
[翻译进度](progress.md)

## 版本
当前最新版与2.2.8相对应

## 参与人
flym

## 不翻译package
1. org.mvel2.templates

    模板相关,即将mvel的各个处理认为是模板方法
2. org.mvel2.sh

    脚本相关,在命令行执行mvel语句
3. org.mvel2.asm

    将asm的类直接copy过来,因此不作处理
    
4. proto相关
    
    原类中有关于各种原型的介绍和处理,但在整个对外文档中均未涉及到.并且这里的原型与function是分开的体系,
其内部也没有什么可介绍的,因此不再尝试进行源码分析.在实际使用中,也没有特别的使用场景.
    
    在此编译中,仅将mvel作为相应的脚本来完成,并不要求其有复杂的计算逻辑,因此其它不再需要的特性则不再作编译和分析.

5. 其它未加说明的指令节点
    
    有一些在外部文档中未说明的场景,如fold,soundlike, strsim等操作节点,以及stacklang,是为
指定的程序场景所设定的,在外部公共api中,也没有此的说明.被认为定制化过强,因此不再编译.
    
    并且,此类的处理,在具体使用mvel中,也可以通过外部方法import的方式来实现相应的功能,并没有特别的不可替代性.

## 其它文档
- mvel的几种运行模式
- MVEL类中的方法分类及作用
- ExecutableStatement的不同子类区分及作用

## 缺陷
- 语法完备性,在数据中进行即时操作问题
