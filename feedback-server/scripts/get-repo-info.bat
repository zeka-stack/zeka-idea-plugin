@echo off
REM 获取 GitHub 仓库信息和讨论类别 ID
REM 使用方法: get-repo-info.bat YOUR_GITHUB_TOKEN
REM
REM 获取 GitHub Token:
REM - Personal Access Token (Classic): https://github.com/settings/tokens
REM - Fine-grained Personal Access Token: https://github.com/settings/tokens?type=beta
REM
REM Token 所需权限:
REM - 对于公共仓库: public_repo 或 Discussions: Read/Write
REM - 对于私有仓库: repo 或 Discussions: Read/Write

if "%1"=="" (
    echo 错误: 请提供 GitHub Token
    echo 使用方法: get-repo-info.bat YOUR_GITHUB_TOKEN
    exit /b 1
)

set GITHUB_TOKEN=%1
set OWNER=zeka-stack
set REPO=zeka-idea-plugin

echo 正在查询仓库信息和讨论类别...
echo 仓库: %OWNER%/%REPO%
echo.

curl -X POST https://api.github.com/graphql ^
  -H "Authorization: Bearer %GITHUB_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"query\": \"query GetRepositoryInfo($owner: String!, $name: String!) { repository(owner: $owner, name: $name) { id name url discussionCategories(first: 10) { nodes { id name description emoji } } } }\", \"variables\": {\"owner\": \"%OWNER%\", \"name\": \"%REPO%\"}}"

echo.
echo 提示:
echo - repository.id 是创建讨论时需要的 repositoryId
echo - discussionCategories.nodes[].id 是创建讨论时需要的 categoryId
echo - 可以根据 category name 选择合适的类别（如 General、Ideas、Q&A 等）

