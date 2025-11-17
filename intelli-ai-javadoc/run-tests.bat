@echo off
REM IntelliJ IntelliAI JavaDoc 插件 - 测试运行脚本 (Windows)
REM 此脚本用于运行所有单元测试

echo ========================================
echo IntelliJ IntelliAI JavaDoc Plugin - 测试运行
echo ========================================
echo.

REM 检查 Gradle 是否存在
if not exist "gradlew.bat" (
    echo 错误: gradlew.bat 文件不存在
    exit /b 1
)

REM 运行测试
echo 正在运行所有单元测试...
echo.

call gradlew.bat clean test --info

REM 检查测试结果
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ 所有测试通过！
    echo.
    echo 测试报告位置:
    echo   build\reports\tests\test\index.html
    echo.
    echo 测试覆盖率报告（如果生成）:
    echo   build\reports\jacoco\test\html\index.html
) else (
    echo.
    echo ✗ 测试失败！
    echo.
    echo 请查看详细错误信息：
    echo   build\reports\tests\test\index.html
    exit /b 1
)

echo.
echo ========================================
echo 测试完成
echo ========================================
pause

