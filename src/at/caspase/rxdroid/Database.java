/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
 * 
 * This file is part of RxDroid.
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package at.caspase.rxdroid;


import java.io.Serializable;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import at.caspase.rxdroid.util.Hasher;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

/**
 * All DB access goes here.
 * 
 * Note that all ORMLite related classes will have members prefixed without the
 * usual "m" (i.e. "comment" instead of "mComment").
 * 
 * @author Joseph Lehner
 *
 */
public class Database 
{
	private static final String TAG = Database.class.getName();
	
	private static HashSet<OnDatabaseChangedListener> sWatchers = new HashSet<OnDatabaseChangedListener>();
	
	/**
	 * Add an object to the DatabseWatcher registry.
	 * 
	 * Whenever the methods create(), update(), or delete() are used, all
	 * objects that were registered using this method will have their
	 * callback functions called accordingly (see DatabaseWatcher).
	 * 
	 * @param watcher
	 */
	public static synchronized void registerOnChangedListener(OnDatabaseChangedListener watcher) {
		sWatchers.add(watcher);		
	}
	
	
	/**
	 * Removes an object from the DatabaseWatcher registry.
	 * 
	 * @param watcher
	 */
	public static synchronized void unregisterOnChangedListener(OnDatabaseChangedListener watcher) {
		sWatchers.remove(watcher);
	}
	
	
	/**
	 * Creates a new database entry.
	 * 
	 * Using this function will ensure that all DatabaseWatcher objects registered
	 * via addWatcher are notified of the change.
	 * 
	 * @param <T>
	 * @param <ID>
	 * @param dao
	 * @param t
	 */
	public static <T extends Entry, ID> void create(final Dao<T, ID> dao, final T t)
	{
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					dao.create(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}				
			}
		});
		
		th.start();
				
		if(t instanceof Drug)
		{
			for(OnDatabaseChangedListener watcher : sWatchers)
				watcher.onCreateEntry((Drug) t);			
		}
		else if(t instanceof Intake)
		{
			for(OnDatabaseChangedListener watcher : sWatchers)
				watcher.onCreateEntry((Intake) t);
		}
	}
	
	public static <T extends Entry, ID> void update(final Dao<T, ID> dao, final T t)
	{
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					dao.update(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}				
			}
		});
		
		th.start();
				
		if(t instanceof Drug)
		{
			for(OnDatabaseChangedListener watcher : sWatchers)
				watcher.onUpdateEntry((Drug) t);			
		}
	}	
	
	public static <T extends Entry, ID> void delete(final Dao<T, ID> dao, final T t)
	{
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					dao.delete(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}				
			}
		});
		
		th.start();
		
		if(t instanceof Drug)
		{
			for(OnDatabaseChangedListener watcher : sWatchers)
				watcher.onDeleteEntry((Drug) t);
		}
		else if(t instanceof Intake)
		{
			for(OnDatabaseChangedListener watcher : sWatchers)
				watcher.onDeleteEntry((Intake) t);
		}
	}
	
	public static void dropDatabase(Helper helper)
	{
		helper.onUpgrade(helper.getWritableDatabase(), 0, Helper.DB_VERSION);
		
		for(OnDatabaseChangedListener watcher : sWatchers)
			watcher.onDatabaseDropped();
	}
	
	public static List<Intake> findIntakes(Dao<Intake, Integer> dao, Drug drug, Date date, int doseTime)
	{		
		try
    	{    		
	    	QueryBuilder<Database.Intake, Integer> qb = dao.queryBuilder();
	    	Where<Database.Intake, Integer> where = qb.where();
	    	where.eq(Database.Intake.COLUMN_DRUG_ID, drug.getId());
	    	where.and();
	    	where.eq(Database.Intake.COLUMN_DATE, (java.util.Date) date);
	    	where.and();
	    	where.eq(Database.Intake.COLUMN_DOSE_TIME, doseTime);
    			        	
	    	return dao.query(qb.prepare());
    	}
    	catch(SQLException e)
    	{
    		throw new RuntimeException(e);
    	}    
		
	}
	
	private Database() {}
	
	/**
	 * Base class for all database entries.
	 * 
	 * The main purpose of this class is to provide alleviate child classes from
	 * declaring an ID field and to provide an unimplemented equals() method.
	 * 
	 * @author Joseph Lehner
	 *
	 */
	public static abstract class Entry implements Serializable
    {
    	private static final long serialVersionUID = 8300191193261799857L;

		public static final String COLUMN_ID = "id";
		
		@DatabaseField(columnName = COLUMN_ID, generatedId = true)
    	protected int id;
		
		/**
		 * This will always throw!
		 * 
		 * @throws UnsupportedOperationException
		 */
		@Override
		public boolean equals(Object other) {
			throw new UnsupportedOperationException();
		}
		
		/**
		 * This will always throw!
		 * 
		 * @throws UnsupportedOperationException
		 */
		@Override
		public int hashCode() {
			throw new UnsupportedOperationException();
		}
		
    	public int getId() {
    		return id;
    	}    	
    }
        
	/**
	 * Class for handling the drug database.
	 * 
	 * The word "dose" in the context of this documentation refers to
	 * the smallest available dose of that drug without having to 
	 * manually reduce its amount (i.e. no pill-splitting). For example,
	 * a package of Aspirin containing 30 tablets contains 30 doses; of
	 * course, the intake schedule may also contain doses in fractions.
	 * 
	 * Another term you'll come across in the docs and the code is the
	 * concept of a 'dose-time'. A dose-time is a user-definable subdivision
	 * of the day, having one of the following predefined names: morning,
	 * noon, evening, night.  
	 * 
	 * Any drug in the database will have the following attributes:
	 * <ul>
	 *  <li>A unique name</li>
	 *  <li>The form of the medication. This will be reflected in the UI by 
	 *      displaying a corresponding icon next to the drug's name.</li>
	 *  <li>The size of one refill. This corresponds to the amount of doses 
	 *      per prescription, package, etc. Note that due to the definition of
	 *      the word "dose" mentioned above, this size must not be a fraction.</li>
	 *  <li>The current supply. This contains the number of doses left for this particular drug.</li>
	 *  <li>An optional comment for that drug (e.g. "Take with food").</li>
	 *  <li>A field indicating whether the drug should be considered active. A drug marked
	 *      as inactive will be ignored by the DrugNotificationService.</li>
	 * </ul>  
	 * 
	 * @author Joseph Lehner
	 *
	 */
	@DatabaseTable(tableName = "drugs")
	public static class Drug extends Entry
	{
	    private static final long serialVersionUID = -2569745648137404894L;
		
	    public static final int FORM_TABLET = 0;
	    public static final int FORM_INJECTION = 1;
	    public static final int FORM_SPRAY = 2;
	    public static final int FORM_DROP = 3;
	    public static final int FORM_GEL = 4;
	    public static final int FORM_OTHER = 5;
	    
	    public static final int TIME_MORNING = 0;
	    public static final int TIME_NOON = 1;
	    public static final int TIME_EVENING = 2;
	    public static final int TIME_NIGHT = 3;
	    public static final int TIME_WHOLE_DAY = 4;
	    	    
	    public static final String COLUMN_NAME = "name";
	  	   	   
	    @DatabaseField(unique = true)
	    private String name;
	    
	    @DatabaseField(useGetSet = true)
	    private int form;
	    
	    @DatabaseField(defaultValue = "true")
	    private boolean active = true;
	    
	    // if mRefillSize == 0, mCurrentSupply should be ignored
	    @DatabaseField(useGetSet = true)
	    private int refillSize;
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE, useGetSet = true)
	    private Fraction currentSupply = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseMorning = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseNoon = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseEvening = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseNight = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseWholeDay = new Fraction();
	    
	    @DatabaseField(canBeNull = true)
	    private String comment;
	    
	    public Drug() {}
	    	    
	    public String getName() {
	        return name;
	    }
	    
	    public int getForm() {
	        return form;
	    }
	    
	    public int getFormResourceId() 
	    {
	    	switch(form)
	    	{
	    		case FORM_INJECTION:
	    			return R.drawable.med_syringe;
	    			
	    		case FORM_DROP:
	    			return R.drawable.med_drink;
	    		
	    		case FORM_TABLET:
	    			// fall through
	    			
	    		default:
	    			return R.drawable.med_pill;	
	    			
	    		// FIXME
	    	}
	    }
	    
	    public boolean isActive() {
	    	return active;
	    }
	    
	    public int getRefillSize() {
	        return refillSize;
	    }
	    
	    public Fraction getCurrentSupply() {
	        return currentSupply;
	    }
	    
	    public Fraction[] getSchedule() {
	        return new Fraction[] { doseMorning, doseNoon, doseEvening, doseNight, doseWholeDay };
	    }
	    
	    public Fraction getDose(int doseTime) 
	    {	    	
	    	final Fraction doses[] = {
	                doseMorning,
	                doseNoon,
	                doseEvening,
	                doseNight,
	                doseWholeDay
	        };
	    	
	    	return doses[doseTime];
	    }
	    
	    public String getComment() {
	        return comment;
	    }
	    
	    public void setName(String name) {
	    	this.name = name;
	    }
	    
	    public void setForm(int form) 
	    {
	        if(form > FORM_OTHER)
	            throw new IllegalArgumentException();
	        this.form = form;
	    }
	    

		public void setActive(boolean active) {
			this.active = active;
		}
	    
	    public void setRefillSize(int refillSize)
	    {
	        if(refillSize < 0)
	            throw new IllegalArgumentException();
	        this.refillSize = refillSize;
	    }
	    
	    public void setCurrentSupply(Fraction currentSupply)
	    {
	        if(currentSupply == null)
	            this.currentSupply = Fraction.ZERO;
	        else if(currentSupply.compareTo(0) == -1)
	            throw new IllegalArgumentException();
	        
	        this.currentSupply = currentSupply;
	    }
	
	    public void setDose(int doseTime, Fraction value) 
	    {
	    	switch(doseTime)
	    	{
	    		case TIME_MORNING:
	    			doseMorning = value;
	    			break;
	    		case TIME_NOON:
	    			doseNoon = value;
	    			break;
	    		case TIME_EVENING:
	    			doseEvening = value;
	    			break;
	    		case TIME_NIGHT:
	    			doseNight = value;
	    			break;
	    		default:
	    			throw new IllegalArgumentException();
	    	}
	    }
	
	    public void setComment(String comment) {
	        this.comment = comment;
	    }	
	    
	    @Override
	    public boolean equals(Object o)
	    {
	    	if(!(o instanceof Drug))
	    		return false;
	    	
	    	final Drug other = (Drug) o;
	    	    	
	    	if(other == this)
	    		return true;
	    	
	    	final Object[] thisMembers = this.getFieldValues();
	    	final Object[] otherMembers = other.getFieldValues();
	    		    	
	    	for(int i = 0; i != thisMembers.length; ++i)
	    	{
	    		if(!thisMembers[i].equals(otherMembers[i]))
	    			return false;
	    	}
	    	
	    	return true;	    	
	    }
	    
	    @Override
	    public int hashCode()
	    {
	    	int result = Hasher.SEED;
	    	
	    	final Object[] thisMembers = this.getFieldValues();
	    	
	    	for(Object o : thisMembers)
	    		result = Hasher.hash(result, o);
	    		    	
	    	return result;
	    }
	    
	    @Override
	    public String toString() {
	    	return name + "(" + id + ")={ " + doseMorning + " - " + doseNoon + " - " + doseEvening + " - " + doseNight + "}";
	    }
	    
	    /**
	     * Get all relevant members for comparison/hashing.
	     * 
	     * When comparing for equality or hashing, we ignore a drug's unique ID, as it may be left
	     * uninitialized and automatically determined by the SQLite logic.
	     * 
	     * @return An array containing all fields but the ID.
	     */	    
	    private Object[] getFieldValues()
	    {
	    	final Object[] members = {
	    		this.name,
	    		this.form,
	    		this.active,
	    		this.doseMorning,
	    		this.doseNoon,
	    		this.doseEvening,
	    		this.doseNight,
	    		this.currentSupply,
	    		this.refillSize,
	    		this.comment	    		
	    	};
	    	
	    	return members;
	    }
	}
	
	/**
	 * Represents a dose intake by the user.
	 * 
	 * Each database entry will consist of an id of the drug that was taken, a timestamp 
	 * representing the time the user marked the dose as 'taken' in the app, the dose-time, the <em>scheduled</em>
	 * date (note that this may differ from the date represented by the timestamp. Assume for
	 * example that the user takes a drug scheduled for the night at 1 minute past midnight.),
	 * 
	 * @author Joseph Lehner
	 */
	@DatabaseTable(tableName = "intake")
	public static class Intake extends Entry
	{
	    private static final long serialVersionUID = -9158847314588407608L;
		
		public static final String COLUMN_DRUG_ID = "drug_id";
		public static final String COLUMN_DATE = "date";
		public static final String COLUMN_TIMESTAMP = "timestamp";
		public static final String COLUMN_DOSE_TIME = "dose_time";
	    
		@DatabaseField(columnName = COLUMN_DRUG_ID, foreign = true)
		private Drug drug;
		
		@DatabaseField(columnName = COLUMN_DATE)
        private java.util.Date date;
		
		@DatabaseField(columnName = COLUMN_TIMESTAMP)
        private java.util.Date timestamp;
		
		@DatabaseField(columnName = COLUMN_DOSE_TIME)
        private int doseTime;
		
		// FIXME add a field for the actual dose that was taken
		
		public Intake() {}
		
		public Intake(Drug drug, Date date, int doseTime) 
		{
			this.drug = drug;
			setDate(date);
			this.timestamp = new Timestamp(System.currentTimeMillis());
			this.doseTime = doseTime;
		}
		
		public Drug getDrug() {
			return drug;
		}

		public Date getDate() {
			return new Date(date.getTime());
		}

		public Timestamp getTimestamp() {
			return new Timestamp(timestamp.getTime());
		}

		public int getDoseTime() {
			return doseTime;
		}

		public void setDrug(Drug drug) {
			this.drug = drug;
		}

		public void setDate(Date date) {
			this.date = new java.util.Date(date.getTime());
		}

		public void setTimestamp(Timestamp timestamp) {
			this.timestamp = new Timestamp(timestamp.getTime());
		}

		public void setDoseTime(int doseTime) {
			this.doseTime = doseTime;
		}
		
		@Override
		public int hashCode()
		{
			int result = Hasher.SEED;
			
			result = Hasher.hash(result, drug);
			result = Hasher.hash(result, date);
			result = Hasher.hash(result, timestamp);
			result = Hasher.hash(result, doseTime);
			
			return result;			
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(!(o instanceof Intake))
				return false;
			
			final Intake other = (Intake) o;
			
			if(this.doseTime != other.doseTime)
				return false;
			
			if(!this.timestamp.equals(other.timestamp))
				return false;
			
			if(!this.date.equals(other.date))
				return false;
			
			if(!this.drug.equals(other.drug))
				return false;
			
			return true;			
		}
				
		@Override
		public String toString() {
			return drug.getName() + ": date=" + date + ", doseTime=" + doseTime;		
		}
		
		
    }
	
	/**
	 * Helper class for ORMLite related voodoo.
	 * 
	 * @author Joseph Lehner
	 * 
	 */	
	public static class Helper extends OrmLiteSqliteOpenHelper
	{
		private static final String DB_NAME = "db.sqlite";
		private static final int DB_VERSION = 39;
		
		private Dao<Database.Drug, Integer> mDrugDao = null;
		private Dao<Database.Intake, Integer> mIntakeDao = null;
				
		public Helper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db, ConnectionSource cs) 
		{
		    try
			{
				TableUtils.createTable(cs, Database.Drug.class);
				TableUtils.createTable(cs, Database.Intake.class);				
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Error while creating tables", e);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion) 
		{
			try
			{
				TableUtils.dropTable(cs, Database.Drug.class, true);
				TableUtils.dropTable(cs, Database.Intake.class, true);
				onCreate(db, cs);
			}
			catch (SQLException e)
			{
				throw new RuntimeException("Error while deleting tables", e);
			}
		}
		
		public void dropTables() {			
			onUpgrade(getWritableDatabase(), 0, DB_VERSION);
		}
			
		public synchronized Dao<Database.Drug, Integer> getDrugDao()
		{
			try
			{
				if(mDrugDao == null)
					mDrugDao = BaseDaoImpl.createDao(getConnectionSource(), Database.Drug.class);
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Cannot get DAO", e);
			}
			return mDrugDao;
		}
		
		public synchronized Dao<Database.Intake, Integer> getIntakeDao()
		{
			try
			{
				if(mIntakeDao == null)
					mIntakeDao = BaseDaoImpl.createDao(getConnectionSource(), Database.Intake.class);
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Cannot get DAO", e);
			}
			return mIntakeDao;
		}
		
		@Override
		public void close()
		{
			super.close();
			mDrugDao = null;
			mIntakeDao = null;
		}
	}
	
	/**
	 * Notifies objects of database changes.
	 * 
	 * Objects implementing this interface and registering themselves with
	 * Database.addWatcher will be notified upon any changes to the database,
	 * as long as they are handled by the functions in Database.
	 * 
	 * @see Database#create
	 * @see Database#update
	 * @see Database#delete
	 * @see Database#dropDatabase
	 * @author Joseph Lehner
	 *
	 */
	public interface OnDatabaseChangedListener
	{
		public void onCreateEntry(Drug drug);
		
		public void onDeleteEntry(Drug drug);
		
		public void onUpdateEntry(Drug drug);
		
		public void onCreateEntry(Intake intake);
		
		public void onDeleteEntry(Intake intake);
		
		public void onDatabaseDropped();	
	}
}
