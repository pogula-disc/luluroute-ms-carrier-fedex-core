package com.luluroute.ms.carrier.repository.query;

public class QueryConstants {

    public static final String FILE_RECORD_EFFECTIVE =
            "SELECT f "
                    + "FROM FuseFileRecordEntity f "
                    + "WHERE :todayDate between f.effectiveDate and f.expirationDate "
                    + "AND f.fileType = :fileType ";

}
