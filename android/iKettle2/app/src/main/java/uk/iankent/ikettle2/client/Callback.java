package uk.iankent.ikettle2.client;

/**
 * Created by iankent on 27/12/2015.
 */
class Callback<T extends KettleResponse> implements OnKettleResponse<T>{
    @Override
    public void onKettleResponse(T response) {
        // do something with result here!
    }
}
