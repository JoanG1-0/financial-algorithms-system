package com.financial.etl.integration;

import com.financial.etl.client.TwelveDataHttpClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Este test de integración descarga datos reales de Twelve Data usando la configuración real (API key en .env)

/* Get-Content .vscode/.env | ForEach-Object {
>>     $name, $value = $_.split('=')
>>     set-item -path env:$name -value $value
>> }
>>
>> .\mvnw.cmd clean verify -Pintegration
*/

@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
    }
)
@Tag("integration")
class TwelveDataHttpClientIT {

    @Autowired
    private TwelveDataHttpClient client;

    @Test
    void shouldDownloadRealDataFromTwelveData() {

        String response = client.downloadTimeSeries("AAPL");

        assertNotNull(response, "La respuesta no debe ser nula");
        assertTrue(response.contains("\"meta\""), "La respuesta debe contener metadatos del activo");
        assertTrue(response.contains("\"values\""), "La respuesta debe contener valores de la serie de tiempo");
    }
}
