Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-ExtentServer.jar", "start", "8080", "D:\Matus\7semester\DS\test"
Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-LockServer.jar", "start", "8081"
Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-DfsServer.jar", "start", "8082", "127.0.0.1:8080", "127.0.0.1:8081"
Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-DfsServer.jar", "start", "8083", "127.0.0.1:8080", "127.0.0.1:8081"
