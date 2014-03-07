package app.philm.in.tasks;

import com.google.common.base.Preconditions;

import com.jakewharton.trakt.entities.CheckinResponse;
import com.jakewharton.trakt.entities.Share;
import com.jakewharton.trakt.services.MovieService;

import app.philm.in.model.PhilmMovie;
import app.philm.in.model.WatchingMovie;
import app.philm.in.network.NetworkError;
import retrofit.RetrofitError;

public class CheckinTraktRunnable extends BaseMovieRunnable<CheckinResponse> {

    private final String mId;
    private final String mMessage;
    private boolean mShareFacebook;
    private boolean mShareTwitter;
    private boolean mSharePath;
    private boolean mShareTumblr;

    public CheckinTraktRunnable(int callingId, String id, String message, boolean shareFacebook,
            boolean shareTwitter, boolean sharePath, boolean shareTumblr) {
        super(callingId);
        mId = Preconditions.checkNotNull(id, "id cannot be null");
        mMessage = message;
        mShareFacebook = shareFacebook;
        mShareTwitter = shareTwitter;
        mSharePath = sharePath;
        mShareTumblr = shareTumblr;
    }

    @Override
    public CheckinResponse doBackgroundCall() throws RetrofitError {
        MovieService.MovieCheckin checkin = new MovieService.MovieCheckin(mId, mMessage, null, null);

        final Share shareConnections = new Share();
        shareConnections.facebook = mShareFacebook;
        shareConnections.twitter = mShareTwitter;
        shareConnections.path = mSharePath;
        shareConnections.tumblr = mShareTumblr;
        checkin.share = shareConnections;

        return getTraktClient().movieService().checkin(checkin);
    }

    @Override
    public void onSuccess(CheckinResponse result) {
        if (RESULT_TRAKT_SUCCESS.equals(result.status)) {
            PhilmMovie movie = getTraktEntityMapper().map(result.movie);

            if (movie != null) {
                long startTime = 0;
                int duration = 0;

                if (result.timestamps != null) {
                    startTime = result.timestamps.start.getTime();
                    duration = result.timestamps.active_for * 1000;
                }

                WatchingMovie watchingMovie = new WatchingMovie(movie, startTime, duration);
                mMoviesState.setWatchingMovie(watchingMovie);
            }
        } else if (RESULT_TRAKT_FAILURE.equals(result.status)) {
            // TODO Check time out, etc
        }
    }

    @Override
    protected int getSource() {
        return NetworkError.SOURCE_TRAKT;
    }
}
