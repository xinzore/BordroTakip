package com.bordrotakip.di

import com.bordrotakip.domain.calendar.TrHolidayCalendar
import com.bordrotakip.domain.calculator.PayrollCalculator
import com.bordrotakip.domain.calculator.PayrollPeriodEngine
import com.bordrotakip.domain.calculator.TaxCalculator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalculatorModule {
    
    @Provides
    @Singleton
    fun provideTaxCalculator(): TaxCalculator {
        return TaxCalculator()
    }
    
    @Provides
    @Singleton
    fun providePayrollPeriodEngine(): PayrollPeriodEngine {
        return PayrollPeriodEngine()
    }
    
    @Provides
    @Singleton
    fun providePayrollCalculator(
        taxCalculator: TaxCalculator,
        holidayCalendar: TrHolidayCalendar
    ): PayrollCalculator {
        return PayrollCalculator(taxCalculator, holidayCalendar)
    }
}
