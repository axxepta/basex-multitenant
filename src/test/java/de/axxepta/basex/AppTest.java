package de.axxepta.basex;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.basex.core.StaticOptions;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.Close;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.DropDB;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.XQuery;
import org.basex.query.QueryException;


public class AppTest {
    //private Context ctx;
    private List<String> userPaths;
    private HashMap<String, Context> userContexts = new HashMap<>(10);
    private ExecutorService es = Executors.newFixedThreadPool(10);
    private String dbName = "stress-test";

    @Before
    public void setup() throws BaseXException {

        userPaths = new ArrayList<>();
        for (int i = 0; i < 10; i++) {

            Context ctx = new Context(); //Context(false) --> do not read from .basex config
            String dbpath =  ctx.soptions.get(StaticOptions.DBPATH);

            String usr = "usr" + i;
            String path = usr + ".xml";
            String userPath = dbName + "/" + path;

            String userDbPath = dbpath + "\\" + usr;
            //set new DBPATH
            ctx.soptions.put(StaticOptions.DBPATH, userDbPath);

            new DropDB(dbName).execute(ctx);
            new CreateDB(dbName).execute(ctx);
            new Open(dbName).execute(ctx);

            new XQuery(String.format("db:add('%s', <container />, '%s')", dbName, path)).execute(ctx);
            userPaths.add(userPath);
            userContexts.put(userPath, ctx);

            new Close().execute(ctx);
        }
    }

    @Test
    public void testParallelUpdates() throws BaseXException, InterruptedException, QueryException {
        for (String path : userPaths) {
            final String p = path;
            Context ctx = userContexts.get(path);
            System.err.println("> submit with DBPATH: " + ctx.soptions.get(StaticOptions.DBPATH));
            es.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        execute(ctx, p);
                    } catch (BaseXException e) {
                        e.printStackTrace();
                    } catch (QueryException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);
    }

    //	@Test
   /* public void testSerialUpdates() throws BaseXException {
        for (String path : userPaths) {
            try {
                execute(ctx, path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }*/

    private void execute(Context ctx, String path) throws BaseXException, QueryException {
        System.err.println("> accessing path: " + path);
        int numObjects = 10;

        for (int i = 0; i < numObjects; i++) {

            new XQuery(String.format("insert node <object id='%d'/> into collection('%s')/container", i, path)).execute(ctx);

        }
        System.err.println("< accessed path: " + path);
    }
}
