package de.axxepta.basex;

import static spark.Spark.*;

import org.basex.core.StaticOptions;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.Close;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.DropDB;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.XQuery;
import org.basex.query.QueryException;

public class App {

    private static String dbName = "stress-test";

    public static void main(String[] args) {

        //get("/hello", (req, res) -> "Hello World");


        get("/collection/:name", (request, response) -> {

            String name = request.params(":name");
            Context ctx = new Context();
            String dbpath =  ctx.soptions.get(StaticOptions.DBPATH);
            String userDbPath = dbpath + "\\" + name;
            //set new DBPATH
            ctx.soptions.put(StaticOptions.DBPATH, userDbPath);

            String res = new XQuery(String.format("collection('%s')/container", App.dbName + "/" + name + ".xml")).execute(ctx);

            response.type("text/xml");

            return res;
        });

        exception(BaseXException.class, (exception, request, response) -> {
            // Handle the exception here

            response.status(404);
            response.body(exception.getMessage());
        });
    }
}