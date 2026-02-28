Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-ExtentServer.jar", "stop", "127.0.0.1:8080", "D:\Matus\7semester\DS\test"
Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-LockServer.jar", "stop", "127.0.0.1:8081"
Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-DfsServer.jar", "stop", "127.0.0.1:8082"
Start-Process "java" -ArgumentList "-jar", "D:\Matus\7semester\DS\ds-2024-dfs\labs-DfsServer.jar", "stop", "127.0.0.1:8083"
