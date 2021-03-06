package it.jaschke.alexandria;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.com.google.zxing.integration.android.IntentIntegrator;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText ean;
    //private Button isbnButton;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";

    String scanResult;

    public AddBook(){ }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);

        ean = (EditText) rootView.findViewById(R.id.ean);
        //isbnButton = (Button) rootView.findViewById(R.id.isbn_button);

        //Custom Code
        //If after the scan we get the ISBN number then we can set ean with that and start the intent
        if(scanResult != null && !scanResult.equals("")){
            Log.v(TAG, "Got the scanResult "+scanResult);
            String eanText =scanResult;
            ean.setText(eanText);
            //catch isbn10 numbers
            if(eanText.length()==10 && !eanText.startsWith("978")){
                eanText="978"+eanText;
            }
            if(eanText.length()<13){
                clearFields();
            }else{
                Log.v(TAG, "Result is fine "+scanResult);
                ean.setText(eanText);
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanText);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);

                //Ankur - As per the documentation this should create  a new Loader or reuse an existing one. Calling restartLoader dint invoke onLoaderFinish method
                AddBook.this.restartLoader();
            }
        }

        /*
        if(isbnButton != null){
            Log.v(TAG, "ISBN button not null");
            isbnButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String eanText =ean.getText().toString();
                    //catch isbn10 numbers
                    if(eanText.length()==10 && !eanText.startsWith("978")){
                        eanText="978"+eanText;
                    }
                    if(eanText.length()<13){
                        clearFields();
                        return;
                    }
                    //Once we have an ISBN, start a book intent
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, eanText);
                    bookIntent.setAction(BookService.FETCH_BOOK);
                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();
                }
            });
        }
        */

        /*Commented by Ankur - this logic is now being handled via a ISBN button click.*/

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String eanText =s.toString();
                //catch isbn10 numbers
                if(eanText.length()==10 && !eanText.startsWith("978")){
                    eanText="978"+eanText;
                }
                if(eanText.length()<13){
                    clearFields();
                    return;
                }
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanText);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();
            }
        });



        /*
        * Added by Ankur
        * This button will trigger the Scan functionality
        * */
        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLoaderManager().destroyLoader(LOADER_ID);
                IntentIntegrator scanIntegrator = new IntentIntegrator(getActivity());
                scanIntegrator.initiateScan();
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "onCreateLoader called");
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }

        //Debug - Try to comment this CursorLoader to see if onLoadFinished is called

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(TAG, "In onLoaderReset");
    }

    /*Ankur - Made changes to handle errors and validate the incoming data*/
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "On Load Finished called");

        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));

        /*Added by Ankur to validate authors*/

        String[] authorsArr;
        if(authors != null && !authors.equals("")) {
            authorsArr = authors.split(",");
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        }

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        //if(Patterns.WEB_URL.matcher(imgUrl).matches()){
        if(imgUrl != null && Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);

        /*Added by Ankur*/
        getLoaderManager().destroyLoader(LOADER_ID);
    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    public void setScanResult(String scanResult) {
        this.scanResult = scanResult;
    }
}