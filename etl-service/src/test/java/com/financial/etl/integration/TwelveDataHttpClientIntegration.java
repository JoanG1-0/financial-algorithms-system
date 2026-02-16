package com.financial.etl.integration;

import com.financial.etl.client.TwelveDataHttpClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


// Este test de integración descarga datos reales de Twelve Data usando la configuración real (API key en .env)

/* Get-Content .vscode/.env | ForEach-Object {
>>     $name, $value = $_.split('=')
>>     set-item -path env:$name -value $value
>> }
>>
>> .\mvnw.cmd -pl etl-service -Dtest=*Integration test
*/

@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
    }
)
@Tag("integration")
class TwelveDataHttpClientIntegration {

    @Autowired
    private TwelveDataHttpClient client;

    @Test
    void shouldDownloadRealDataFromTwelveData() {

        String response = client.downloadTimeSeries("AAPL");

        System.out.println("Respuesta real de Twelve Data:");
        System.out.println(response.substring(0, 500));
    }
}
