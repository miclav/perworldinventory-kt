package me.ebonjaeger.perworldinventory.service

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import me.ebonjaeger.perworldinventory.configuration.PlayerSettings
import me.ebonjaeger.perworldinventory.configuration.Settings
import me.ebonjaeger.perworldinventory.data.PlayerProfile
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.ServicesManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

/**
 * Test for [EconomyService].
 */
@PrepareForTest(Settings::class, PlayerProfile::class)
@RunWith(PowerMockRunner::class)
class EconomyServiceTest {

    @InjectMocks
    private lateinit var economyService: EconomyService

    @Mock
    private lateinit var server: Server
    @Mock
    private lateinit var settings: Settings
    @Mock
    private lateinit var pluginManager: PluginManager
    @Mock
    private lateinit var servicesManager: ServicesManager
    @Mock
    private lateinit var registeredServiceProvider: RegisteredServiceProvider<Economy>
    @Mock
    private lateinit var economy: Economy
    @Mock
    private lateinit var playerProfile: PlayerProfile

    @Before
    fun wireUpMocks() {
        given(server.servicesManager).willReturn(servicesManager)
        given(server.pluginManager).willReturn(pluginManager)
        given(pluginManager.getPlugin("Vault")).willReturn(mock(Plugin::class.java))
        given(servicesManager.getRegistration(Economy::class.java)).willReturn(registeredServiceProvider)
        given(registeredServiceProvider.provider).willReturn(economy)
    }

    @Test
    fun shouldInitializeEconomy() {
        // given
        given(settings.getProperty(PlayerSettings.USE_ECONOMY)).willReturn(true)

        // when
        economyService.tryLoadEconomy()

        // then
        assertThat(getEconomyField(), present())
    }

    @Test
    fun shouldNotInitializeEconomyIfSoConfigured() {
        // given
        given(settings.getProperty(PlayerSettings.USE_ECONOMY)).willReturn(false)

        // when
        economyService.tryLoadEconomy()

        // then
        assertThat(getEconomyField(), absent())
        verifyZeroInteractions(servicesManager, registeredServiceProvider)
    }

    @Test
    fun shouldWithdrawFunds() {
        // given
        given(settings.getProperty(PlayerSettings.USE_ECONOMY)).willReturn(true)
        economyService.tryLoadEconomy()

        val player = mock(Player::class.java)
        given(economy.getBalance(player)).willReturn(320.0)

        // when
        economyService.withDrawMoneyFromPlayer(player)

        // then
        verify(economy).withdrawPlayer(player, 320.0)
    }

    @Test
    fun newBalanceGreaterThanOldBalance() {
        // given
        given(settings.getProperty(PlayerSettings.USE_ECONOMY)).willReturn(true)
        economyService.tryLoadEconomy()

        val player = mock(Player::class.java)
        val oldBalance = 2.81
        val newBalance = 1332.49
        given(economy.getBalance(player)).willReturn(oldBalance)
        given(playerProfile.balance).willReturn(newBalance)

        // when
        economyService.setNewBalance(player, playerProfile.balance)

        // then
        verify(economy).depositPlayer(player, (newBalance - oldBalance))
    }

    @Test
    fun newBalanceLessThanOldBalance() {
        // given
        given(settings.getProperty(PlayerSettings.USE_ECONOMY)).willReturn(true)
        economyService.tryLoadEconomy()

        val player = mock(Player::class.java)
        val oldBalance = 1332.49
        val newBalance = 2.81
        val er = EconomyResponse((oldBalance - newBalance), newBalance, EconomyResponse.ResponseType.SUCCESS, "Woo?")
        given(economy.getBalance(player)).willReturn(oldBalance)
        given(playerProfile.balance).willReturn(newBalance)
        given(economy.withdrawPlayer(player, (oldBalance - newBalance))).willReturn(er)

        // when
        economyService.setNewBalance(player, playerProfile.balance)

        // then
        verify(economy).withdrawPlayer(player, (oldBalance - newBalance))
    }

    private fun getEconomyField(): Economy? {
        return economyService::class.java.getDeclaredField("economy").let {
            it.isAccessible = true
            return@let it.get(economyService) as Economy?
        }
    }
}
