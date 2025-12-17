package com.novaswap.api.dto;

import com.novaswap.model.Network;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkResponse {
    private Network currentNetwork;
    private List<Network> supportedNetworks;
}
