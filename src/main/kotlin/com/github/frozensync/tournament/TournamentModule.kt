package com.github.frozensync.tournament

import com.github.frozensync.tournament.raspberrypi.RaspberryPiService
import com.github.frozensync.tournament.raspberrypi.RaspberryPiServiceImpl
import org.koin.dsl.module

val tournamentModule = module {
    single<TournamentService> { TournamentServiceImpl(get()) }
    single<RaspberryPiService> { RaspberryPiServiceImpl(get(), get()) }
}
