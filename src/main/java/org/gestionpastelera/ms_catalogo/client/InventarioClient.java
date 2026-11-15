package org.gestionpastelera.ms_catalogo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-inventario", url = "${services.inventario.url}")
public interface InventarioClient {

    @PostMapping("/api/v1/inventario/stock/bulk")
    Map<String, Integer> obtenerStockBulk(@RequestBody List<String> codigos);
}
