package cz.eida.minecraft.sipauth.utils;

import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * UUID handler tests.
 */
public class TestUUID {

    @Test
    public void offlineMatch() {
        UUID Eida_cz = UUID.fromString("dc1c46e7-8c4d-3676-bc94-fc4aaf0595ce");

        assertEquals(Eida_cz, UUIDManager.generatePlayerUUID("Eida_cz"));
    }

    @Test
    public void onlineMatch() throws IOException {
        UUID Eida_cz = UUID.fromString("3434d50d-df4f-44fc-8ef4-aac0433388d2");

        assertEquals(Eida_cz, UUIDManager.fetchPlayerUUID("Eida_cz"));
    }
}
