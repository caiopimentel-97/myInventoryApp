package com.example.caioalvesdasilva.inventoryapp.data;

/**
 * Created by caio.alves.da.silva on 21/08/2017.
 */

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.IllegalFormatException;

/**
 * {@link ContentProvider} for car app.
 */
public class CarProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = CarProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the cars table */
    private static final int CARS = 100;

    /** URI matcher code for the content URI for a single car in the cars table */
    private static final int CAR_ID = 101;

    /** UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.*/
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form
        // "content://com.example.caioalvesdasilva.inventoryapp/cars" will map to the
        // integer code {@link #CARS}. This URI is used to provide access to MULTIPLE rows
        // of the cars table.
        sUriMatcher.addURI(CarContract.CONTENT_AUTHORITY, CarContract.PATH_CARS, CARS);

        // The content URI of the form
        // "content://com.example.caioalvesdasilva.inventoryapp/cars/#" will map to the
        // integer code {@link #CAR_ID}. This URI is used to provide access to ONE single row
        // of the cars table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.caioalvesdasilva.inventoryapp/cars/3" matches, but
        // "content://com.example.caioalvesdasilva.inventoryapp/cars"
        // (without a number at the end) doesn't match.
        sUriMatcher.addURI(CarContract.CONTENT_AUTHORITY, CarContract.PATH_CARS + "/#", CAR_ID);
    }

    /** Database helper object */
    private CarDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new CarDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case CARS:
                // For the CARS code, query the cars table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the cars table.
                cursor = database.query(CarContract.CarEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                    break;
            case CAR_ID:
                // For the CAR_ID code, extract out the ID from the URI.
                // For an example URI such as
                // "content://com.example.caioalvesdasilva.inventoryapp/cars/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = CarContract.CarEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the cars table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(CarContract.CarEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CARS:
                return insertCar(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a car into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertCar(Uri uri, ContentValues values) {
        // Check that the brand is not null
        String brand = values.getAsString(CarContract.CarEntry.COLUMN_CAR_BRAND);
        if (brand == null) {
            throw new IllegalArgumentException("Car requires a brand");
        }

        // Check that the model is not null
        String model = values.getAsString(CarContract.CarEntry.COLUMN_CAR_MODEL);
        if (model == null) {
            throw new IllegalArgumentException("Car requires a model");
        }

        // Check that the fuel is valid
        Integer fuel = values.getAsInteger(CarContract.CarEntry.COLUMN_CAR_FUEL);
        if (fuel == null || !CarContract.CarEntry.isValidFuel(fuel)) {
            throw new IllegalArgumentException("Car requires valid fuel");
        }

        // If the mileage is provided, check that it's greater than or equal to 0 km
        Integer mileage = values.getAsInteger(CarContract.CarEntry.COLUMN_CAR_MILEAGE);
        if (mileage != null && mileage < 0) {
            throw new IllegalArgumentException("Car requires valid mileage");
        }

        // If the quantity is provided, check that it's greater than or equal to 0
        Integer quantity = values.getAsInteger(CarContract.CarEntry.COLUMN_CAR_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Car requires valid quantity");
        }

        // If the price is provided, check that it's greater than or equal to U$0
        Integer price = values.getAsInteger(CarContract.CarEntry.COLUMN_CAR_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Car requires valid price");
        }

        // Check that the car contact supplier name is not null
        String supplierName = values.getAsString(CarContract.CarEntry.COLUMN_SUPPLIER_NAME);
        if (supplierName == null) {
            throw new IllegalArgumentException("Car requires a supplier contact name");
        }


        // Check that the car contact supplier email is not null
        String supplierEmail = values.getAsString(CarContract.CarEntry.COLUMN_SUPPLIER_EMAIL);
        if (supplierEmail == null) {
            throw new IllegalArgumentException("Car requires a supplier contact email");
        }

        // No need to check the brand, any value is valid (including null).
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();


        // Insert the new cars with the given values
        long id = database.insert(CarContract.CarEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the car content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CARS:
                return updateCar(uri, contentValues, selection, selectionArgs);
            case CAR_ID:
                // For the CAR_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = CarContract.CarEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateCar(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update cars in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more cars).
     * Return the number of rows that were successfully updated.
     */
    private int updateCar(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        //if there are no changes
        if (values == null) {

            // If the {@link CarEntry#COLUMN_CAR_BRAND} key is present,
            // check that the name value is not null.
            if (values.containsKey(CarContract.CarEntry.COLUMN_CAR_BRAND)) {
                String brand = values.getAsString(CarContract.CarEntry.COLUMN_CAR_BRAND);
                if (brand == null) {
                    throw new IllegalArgumentException("Car requires a brand");
                }
            }

            // If the {@link CarEntry#COLUMN_CAR_MODEL} key is present,
            // check that the name value is not null.
            if (values.containsKey(CarContract.CarEntry.COLUMN_CAR_MODEL)) {
                String model = values.getAsString(CarContract.CarEntry.COLUMN_CAR_BRAND);
                if (model == null) {
                    throw new IllegalArgumentException("Car requires a model");
                }
            }

            // If the {@link CarEntry#COLUMN_SUPPLIER_NAME} key is present,
            // Check that the car contact supplier  is not null
            if (values.containsKey(CarContract.CarEntry.COLUMN_SUPPLIER_NAME)) {
                String supplierName = values.getAsString(CarContract.CarEntry.COLUMN_SUPPLIER_NAME);
                if (supplierName == null) {
                    throw new IllegalArgumentException("Car requires a supplier contact");
                }
            }

            // If the {@link CarEntry#COLUMN_SUPPLIER_EMAIL} key is present,
            // Check that the car contact supplier  is not null
            if (values.containsKey(CarContract.CarEntry.COLUMN_SUPPLIER_EMAIL)) {
                String supplierEmail = values.getAsString(CarContract.CarEntry.COLUMN_SUPPLIER_EMAIL);
                if (supplierEmail == null) {
                    throw new IllegalArgumentException("Car requires a supplier email");
                }
            }

            // If the {@link CarEntry#COLUMN_CAR_FUEL} key is present,
            // check that the fuel value is valid.
            if (values.containsKey(CarContract.CarEntry.COLUMN_CAR_FUEL)) {
                Integer fuel = values.getAsInteger(CarContract.CarEntry.COLUMN_CAR_FUEL);
                if (fuel == null || !CarContract.CarEntry.isValidFuel(fuel)) {
                    throw new IllegalArgumentException("Car requires valid fuel");
                }
            }

            // If the {@link CarEntry#COLUMN_CAR_MILEAGE} key is present,
            // check that the weight value is valid.
            if (values.containsKey(CarContract.CarEntry.COLUMN_CAR_MILEAGE)) {
                // Check that the weight is greater than or equal to 0 km
                Integer mileage = values.getAsInteger(CarContract.CarEntry.COLUMN_CAR_MILEAGE);
                if (mileage != null && mileage < 0) {
                    throw new IllegalArgumentException("Car requires valid mileage");
                }
            }

            // If the {@link CarEntry#COLUMN_CAR_QUANTITY} key is present,
            // check that the quantity value is valid.
            if (values.containsKey(CarContract.CarEntry.COLUMN_CAR_QUANTITY)) {
                // Check that the quantity is greater than or equal to 0
                Integer quantity = values.getAsInteger(CarContract.CarEntry.COLUMN_CAR_QUANTITY);
                if (quantity != null && quantity < 0) {
                    throw new IllegalArgumentException("Car requires valid quantity");
                }

                //Check that the car image is not null
                if (values.containsKey(CarContract.CarEntry.COLUMN_CAR_IMAGE)) {
                    throw new IllegalArgumentException("Car requires an image");
                }
            }
            // If there are no values to update, then don't try to update the database
            if (values.size() == 0) {
                return 0;
            }
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(CarContract.CarEntry.TABLE_NAME,
                values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CARS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(CarContract.CarEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CAR_ID:
                // Delete a single row given by the ID in the URI
                selection = CarContract.CarEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(CarContract.CarEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CARS:
                return CarContract.CarEntry.CONTENT_LIST_TYPE;
            case CAR_ID:
                return CarContract.CarEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}