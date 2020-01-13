package com.ulan.app.quotes.ui.main;


import com.ulan.app.quotes.data.QuoteModel;

import java.util.List;

import io.reactivex.Observable;


public interface MainView {

    void setListOfQuotes(Observable<List<QuoteModel>> listOfQuotes);

}