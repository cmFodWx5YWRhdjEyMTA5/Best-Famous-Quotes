package com.lessons.firebase.quotes.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lessons.firebase.quotes.R;
import com.lessons.firebase.quotes.adapter.QuoteAdapter;
import com.lessons.firebase.quotes.data.QuoteData;
import com.lessons.firebase.quotes.di.components.HomeComponent;
import com.lessons.firebase.quotes.di.modules.source.SharedPrefModule;
import com.lessons.firebase.quotes.di.modules.uimodules.HomeModule;
import com.lessons.firebase.quotes.ui.base.BaseFragment;
import com.lessons.firebase.quotes.ui.starred.StarredFragment;
import com.lessons.firebase.quotes.ui.mainactivity.MainActivity;
import com.lessons.firebase.quotes.utils.listeners.FragmentLifecycle;
import com.lessons.firebase.quotes.utils.listeners.ShareObservableListener;

import java.util.List;

import io.reactivex.Observable;

import static android.view.View.GONE;
import static com.lessons.firebase.quotes.utils.Constants.TAG_OTHER;
import static com.lessons.firebase.quotes.utils.Constants.TAG_STATE;

public class HomeFragment extends BaseFragment implements HomeFragmentView, FragmentLifecycle, ShareObservableListener {

    private HomeComponent mHomeComponent;
    private HomeFragmentPresenterImpl mPresenter;
    private SharedPreferences mPreferences;
    private StarredFragment mStarredFragment;
    private RecyclerView mRecyclerView;
    private QuoteAdapter mAdapter;

    private BottomNavigationView mBottomNavigationView;
    private Animation mAnimationBottomHide;
    private Animation mAnimationBottomShow;

    private Observable<List<QuoteData>> mListObservable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ((MainActivity) getActivity()).setOnSendListener(this);

        initListComponent();
        mPresenter.loadQuotes(mListObservable);
    }

    private void initListComponent() {
        mHomeComponent = getAppComponent().listBuilder()
                .mainModule(new HomeModule(this))
                .sharedModule(new SharedPrefModule(getActivity()))
                .build();
        mHomeComponent.inject(this);

        mPresenter = mHomeComponent.getPresenter();
        mPreferences = mHomeComponent.getSharedPreference();
        mListObservable = mHomeComponent.getObservableList();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_fragment, container, false);

        mBottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);
        mAnimationBottomHide = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_hide_anim);
        mAnimationBottomShow = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_show_anim);

        mRecyclerView = view.findViewById(R.id.recycler_view_quotes);
        mRecyclerView.addOnScrollListener(recyclerScrollListener);
        return view;
    }

    private RecyclerView.OnScrollListener recyclerScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    }
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0 && mBottomNavigationView.isShown()) {
                        mBottomNavigationView.setVisibility(GONE);
                        mBottomNavigationView.startAnimation(mAnimationBottomHide);
                    } else if (dy < 0 && !mBottomNavigationView.isShown()) {
                        mBottomNavigationView.setVisibility(View.VISIBLE);
                        mBottomNavigationView.startAnimation(mAnimationBottomShow);
                    }
                }
            };

    private void sendData(QuoteData quoteData) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString("image", quoteData.getUrlImage());
        editor.putString("quote", quoteData.getQuote());
        editor.putString("author", quoteData.getAuthor());
        editor.putInt("id", quoteData.getId());
        editor.putInt("liked", quoteData.getIsLiked());
        editor.apply();
    }

    @Override
    public void showQuotes(List<QuoteData> listQuotes) {
        mAdapter = new QuoteAdapter(getActivity(), listQuotes, position -> {
            QuoteData quoteData = listQuotes.get(position);
            sendData(quoteData);
            if (mStarredFragment != null) {
                mStarredFragment.setDataShared();
               Toast.makeText(getActivity(), "Quote added to Starred", Toast.LENGTH_SHORT).show();
            }

        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void showQuotesError(Throwable e) {
        Log.d(TAG_OTHER, "showQuotesError: " + e.getMessage());
        Toast.makeText(getActivity(), "Please connect your phone to internet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPauseFragment(Fragment fragment) {
        if (fragment instanceof StarredFragment) {
            mStarredFragment = (StarredFragment) fragment;
            Log.d(TAG_STATE, "onPauseFragment: HomeFragment " + fragment);
        }
    }

    @Override
    public void onResumeFragment() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        Log.d(TAG_STATE, "onResumeFragment: HomeFragment ");
    }

    @Override
    public void passObservable(Observable<List<QuoteData>> listObservable) {
        if (listObservable == null) {
            Log.d(TAG_OTHER, "passObservable: Observable<List<QuoteData>> is null object");
        }
        mPresenter.loadQuotes(listObservable);
    }
}