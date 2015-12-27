package uk.iankent.ikettle2.client;

/**
 * Created by iankent on 27/12/2015.
 */
public interface OnKettleResponse<T extends KettleResponse>{
    void onKettleResponse(T response);
}
