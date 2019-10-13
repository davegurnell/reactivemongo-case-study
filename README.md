# ReactiveMongo Case Study

## Getting Started

We'll start by installing things.
Then we'll look through and run an example program
before we start writing code of our own.

## Exercise: Installing Things

If you don't have MongoDB already, 
install the MongoDB macOS app:

```
brew cask install mongodb
```

Open the app. Fiddle with your security settings if you have to.
You should see a leaf icon in your menu bar. 
MongoDB is now available on port 27017.

You can add the `mongo` client app to your `PATH` 
by adding the following to your bash configuration:

```
export PATH="$PATH:/Applications/MongoDB.app/Contents/Resources/Vendor/mongodb/bin"
```

Type `mongo` on the command line 
to verify you've set the path up correctly:

```
bash$ mongo
MongoDB shell version v4.0.8
connecting to: mongodb://127.0.0.1:27017/?gssapiServiceName=mongodb
Implicit session: session { "id" : UUID("25d92942-505f-406f-b5bc-f4bec98af08f") }
MongoDB server version: 4.0.8
# etc...

> exit
bye

bash$
```

## Example: Overview

(This code is adapted from the opening example in 
the [ReactiveMongo documentation](https://reactivemongo.org/releases/0.11/documentation/tutorial/getstarted.html).) 

Before we start writing code, 
we'll go through a quick overview
of connecting to and querying a MongoDB database.
We'll run the code and verify its effect on MongoDB
using the command line client.

Open the file `snacks/src/main/scala/code/Main.scala` 
and take a look at the contents.
Let's go through it below...

### Connecting to MongoDB

Connecting to a database involves three steps:

1. Create a `MongoDriver`
2. Use the driver to open a `MongoConnection` to a server
3. Use the connection to select a `DefaultDB`
4. Use the database to query a collection

Each of these steps is either asynchronous (resulting in a `Future`) or synchronous but fallible (resulting in a `Try`). Here's code for the steps 1 to 3:

```
// Step 1
val driver = MongoDriver()

// Step 2
def openConnection(driver: MongoDriver): Try[MongoConnection] =
  for {
    uri  <- MongoConnection.parseURI(s"mongodb://localhost:27017")
    conn <- driver.connection(uri, strictUri = true)
  } yield conn

// Step 3
def openSnackDatabase(connection: MongoConnection): Future[DefaultDB] =
  connection.database("snackshop")
```

There are heaps of configuration options here that we're skipping over. These can either be provided via query parameters to the connection URI, or via an extra parameter to the `driver.connection()` method. See the [ReactiveMongo](http://reactivemongo.org/releases/0.1x/documentation/tutorial/connect-database.html) docs for more information.

### Querying Collections

Once we have a `DefaultDB`, we can access collections and run queries against them. Here are a couple of examples:

```
// Inserting a record:
def insertSnack(database: DefaultDB, snack: Snack): Future[Unit] =
  database
    .collection[BSONCollection]("snacks")
    .insert.one(snack).map(_ => ()))

// Selecting all (ok... up to 100) records:
def findAllSnacks(database: DefaultDB): Future[List[Snack]] = {
  val errorHandler: Cursor.ErrorHandler[List[Snack]] =
    Cursor.FailOnError[List[Snack]]()

  database
    .collection[BSONCollection]("snacks")
    .find(query = BSONDocument(), projection = None)
    .cursor[Snack]()
    .collect[List](100, errorHandler)
}
```

These examples make use of a tasty test class representing a `Snack`. In addition to the class itself, we have to define instances of two type classes, `BSONDocumentWriter` and `BSONDocumentReader`, that control writing and reading to/from BSON:

```
package code

import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

case class Snack(
  name: String,
  tastiness: Int,
  healthy: Boolean
)

object Snack {
  implicit val writer: BSONDocumentWriter[Snack] =
    Macros.writer

  implicit val reader: BSONDocumentReader[Snack] =
    Macros.reader
}
```

### Putting it All Together

These steps all return instances of `Future` or `Try`
(which can easily be turned into a `Future`). 
We can sequence them into a complete program 
using `map` and `flatMap` or, more conveniently, 
a `for` comprehension:

```
def program: Future[List[Snack]] =
  for {
    conn <- Future.fromTry(openConnection(driver))
    db <- openSnackDatabase(conn)
    _ <- Future.traverse(snacks)(insertSnack(db, _)
    snacks <- selectAllSnacks(db)
  } yield snacks
```

As the code suggests, this program 
connects to our `snackshop` database, 
inserts a bunch of test data, 
and queries it back into memory again.

We can run the program by calling it 
in our `main()` method and awaiting its result:

```
def main(args: Array[String]): Unit = {
  println(Await.result(program, 5.seconds))
}
```

Running the code you should see the following:

```
bash$ sbt run
[info] Loading settings for project global-plugins from idea.sbt,plugins.sbt,metals.sbt ...
[info] Loading global plugins from /Users/dave/.sbt/1.0/plugins
[info] Loading settings for project reactivemongo-case-study-build from plugins.sbt ...
[info] Loading project definition from /Users/dave/dev/projects/reactivemongo-case-study/project
[info] Loading settings for project reactivemongo-case-study from build.sbt ...
[info] Set current project to reactivemongo-case-study (in build file:/Users/dave/dev/projects/reactivemongo-case-study/)
[info] Compiling 1 Scala source to /Users/dave/dev/projects/reactivemongo-case-study/target/scala-2.12/classes ...
[info] running code.Demo
List(Snack(Chocolate,8,false), Snack(Crisps,8,false), Snack(Celery,4,true))
[INFO] [10/10/2019 18:20:31.506] [reactivemongo-akka.actor.default-dispatcher-2] [akka://reactivemongo/user/Connection-1] Message [reactivemongo.core.actors.ChannelDisconnected] without sender to Actor[akka://reactivemongo/user/Connection-1#714486086] was not delivered. [1] dead letters encountered. If this is not an expected behavior, then [Actor[akka://reactivemongo/user/Connection-1#714486086]] may have terminated unexpectedly, This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
[INFO] [10/10/2019 18:20:31.507] [reactivemongo-akka.actor.default-dispatcher-2] [akka://reactivemongo/user/Connection-1] Message [scala.Tuple2] from Actor[akka://reactivemongo/user/Connection-1#714486086] to Actor[akka://reactivemongo/user/Connection-1#714486086] was not delivered. [2] dead letters encountered. If this is not an expected behavior, then [Actor[akka://reactivemongo/user/Connection-1#714486086]] may have terminated unexpectedly, This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
[success] Total time: 8 s, completed 10-Oct-2019 18:20:35

bash$
```

There's a lot of logging output here 
that I haven't worked out how to suppress. 
If you ignore that, though, 
the main line of output looks like this:

```
List(Snack(Chocolate,8,false), Snack(Crisps,8,false), Snack(Celery,4,true))
```

It appears our code has worked.

### Verifying the Results

We can check the data our script inserted into the database 
using the MongoDB client app:

- Run the `mongo` command
- Use the `show dbs` command to verify that `snackshop` exists
- Use the `use snackshow` command to switch to the database
- Use the `db.getCollectionNames()` method 
  to check the `snacks` collection is there
- Use the `db.snacks.find()` command 
  to print all of the records in the collection

```
bash$ mongo
MongoDB shell version v4.0.8
connecting to: mongodb://127.0.0.1:27017/?gssapiServiceName=mongodb
MongoDB server version: 4.0.8

> show dbs
admin      0.000GB
config     0.000GB
local      0.000GB
snackshop  0.000GB

> use snackshop
switched to db snackshop

> db.getCollectionNames()
[ "snacks" ]

> db.snacks.find()
{ "_id" : ObjectId("5d9f59d128fec8a3961a8168"), "name" : "Chocolate", "tastiness" : 8, "healthy" : false }
{ "_id" : ObjectId("5d9f59d128fec8a3961a816a"), "name" : "Crisps", "tastiness" : 8, "healthy" : false }
{ "_id" : ObjectId("5d9f59d128fec8a3961a816c"), "name" : "Celery", "tastiness" : 4, "healthy" : true }

> exit
bye
```

## Exercise. Inserting Data

Let's get started writing some code.
We'll switch from the `snacks` project
to the `olympics` project next to it.

Check the contents of `olympics/src/main/scala/Main.scala`.
There's some minimal code here 
to read a CSV file full of olympic medal winners.
We'll write some code to ingest this into MongoDB.

Rehash the code from `snacks`
to write a short command line application
to populate a collection with 
some CSV data about olympic medal winners.
Call your database `olympics` and your collection `medals`.

When you're done, verify the results of your script
using the `mongo` command line client:

```bash
$ mongo
$ use <DBNAME>
$ db.getCollection("<COLLNAME>").findAll()
```

### Data Credit

The raw CSV data found in `src/main/resources/medals-expanded.csv`
is taken from Tomas Petricek's datavis project, 
[The Gamma](https://thegamma.net).
Tomas in turn adapted it from a dataset from [The Guardian]().
See the [about page](https://rio2016.thegamma.net/about-the-data) 
on The Gamma's web site for more information.)

## Using ReactiveMongo in a Play Application

The `olympicsApi` project shows a minimal setup 
using Play and ReactiveMongo:

- We've included `play-reactivemongo` 
  in our library dependencies in `build.sbt`.
- We've *not* included `reactivemongo` in our dependencies
  because it is transitively brought in by `play-reactivemongo`.
- We've added `play.modules.reactivemongo.ReactiveMongoModule`
  to our list of Play modules in `application.conf`.
  This registers the components we need with Guice.
- We've specified a Mongo connection string in the `mongodb.uri`
  setting in `application.conf`.
  (There are also ways of specifying multiple data sources.)
  
With all of this in place, we can inject a `ReactiveMongoApi`
into any part of our Play application. The `database` method 
of this object gives us a `Future[DefaultDB]`,
which allows us to run queries as we did in our command line app.

### Exercise. Query the Database from Play

Implement code in the `MedalsController.search` endpoint 
to query the collection we set up in part 2.

Note that ReactiveMongo will return the data to us as
a `List` (or `Iterator` etc) of `BSONDocuments`.
You'll need to convert the documents to something nicer 
like a `String`. Do this in any way you can for now.

Return the queried data to the user in any format you like.
We'll concentrate on how to parse content properly next.

### Exercise. Interface using Play JSON.

To integrate ReactiveMongo with Play framework, 
we add a bridging library called `play-reactivemongo` 
to our build:

```
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.18.7-play27"
)
```

We need to add a line to `application.conf` 
to allow Guice to locate the module: 

```
play.modules.enabled += "play.modules.reactivemongo.ReactiveMongoModule"
```

Once we've done these two things, 
we can `@Inject()` a `ReactiveMongoApi` object 
into any part of our Play app:

```
import javax.inject.Inject
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi

class MyController @Inject() (
  components: ControllerComponents, 
  reactiveMongo: ReactiveMongoApi
) extends AbstractController(components) {
  // etc...
}
```

The `ReactiveMongoApi` acts a bit like 
the `DefaultDB` object from our command line script.
We can configure which database it will connect to 
via `application.conf`:

```
mongodb.uri = "mongodb://localhost:27017/olympics"
```

There are, of course, ways of configuring and injecting
different `ReactiveMongoApis` for different databases
via annotations on the constructor parameters. 

We can get hold of a `Future[DefaultDB]` 
via the `reactiveMongoApi.database` method:

```
val db: Future[DefaultDB] = reactiveMongoApi.database
```

Once we have our `DefaultDB` we can query as usual:

```
def allMedals: Future[List[BSONDocument]] =
  for {
    db     <- reactiveMongoApi.database
    medals <- db.collection[BSONCollection]("medals")
                .find(selector = BSONDocument(), projection = None)
                .cursor[BSONDocument]()
                .collect[List](-1, Cursor.FailOnError())
  } yield medals
```

### Exercise. Parameterised Queries

Add optional query parameters to allow the user to control:

- The `order` the results are returned in 
  (allow a minimum of "Year" and "Medal" as options)
- A `count` and `start` index (to allow paging)
- A `year` and `medal` (bronze, silver, or gold) to filter by
- (Optional) A set of fields to return

### Exercise. Typed Results

We can interpret documents from our query results 
as Scala datatypes in one of two ways:

- Treat the collection as a `BSONCollection`,
  query it for `BSONDocuments`,
  and use ReactiveMongo's `BSONReader` type class to parse them.

- Treat the collection as a `JSONCollection`,
  query it for `JsObjects`,
  and use Play's `Reads` type class to parse them.

Feel free to use either option here.
If you're familiar with Play, the latter option will be easier.

Define a `Medal` case class containing 
two or three interesting fields from the dataset.
Write an instance of `BSONReader` or `Reads` 
to parse the incoming document.

Be wary of the capitalisation of 
the fields in the database and 
the fields in your case class.
You'll need to handle any discrepancies 
(e.g. `"Year"` versus `year`)
using the `BSONReader` or `Reads` libraries.

## Exercise. Streaming Data

We'll finish by taking a quick look at streaming data
(to be continued in a workshop on Akka Streams).

As its name suggests, 
ReactiveMongo is capable of producing 
reactive streams of documents out of MongoDB. 
Play can absorb these and 
send streaming responses back to the client.

Duplicate your `search` action 
and create a second action called `stream`.
This should be nearly identical to `search`,
but it should return a stream of JSON documents
instead of a single JSON array.

To create this stream of documents we will call 
the `cursor.documentSource()` method:

```
db.collection[JSONCollection]("medals")
  .find(Json.obj(), projection = Option.empty[JsObject])
  .cursor[JsObject]()
  .documentSource()
```

In order for this to compile 
we need to do two more things...

First, we need an extra import. 
The `documentSource()` method 
isn't actually a method on `Cursor`.
It's an extension method that is enabled via an import 
(this all seems complicated to me too):

```
import reactivemongo.akkastream.cursorProducer
```

Second, the `documentSource()` method itself 
takes an implicit parameter of type `Materializer`. 
`Materializers` are like 
the `ExecutionContexts` of Akka Streams --
they specify how to implement a stream in terms of
an underlying execution model that we don't need to understand. 
We introduce `Materializers` in Play 
by injecting them as implicit parameters 
to the class constructor:

```
class MedalsController @Inject() (
  components: ControllerComponents,
  reactiveMongoApi: ReactiveMongoApi
)(implicit 
  ec: ExecutionContext, 
  mat: Materializer
) extends AbstractController(components) {
  // etc...
}
```

The `cursorProducer` import above is part of ReactiveMongo
and the implicit `Materializer` is part of Akka Streams.
With these two pieces of code in place, 
we can use the `Ok.streamed()` method 
to consume the stream into a response:

```
Ok.streamed(source, None, None)
```

The second parameter to `streamed()` 
represents the content length 
and the third the content type.

Get your `stream` endpoint working using the hints above.
Test the results in the browser 
and note the slight differences the output JSON
compared to the `search` endpoint defined above.

Note that Play infers our content type as `application/json`
but it is technically something else.
I'm fairly new to streamed results,
but there appear to be a few different content-types
for streams with different delimiters and embedded metadata.
I'm not sure which formats tend to be more popular
and which content types to use for each.
However, it seems like it should be pretty easy 
to support all of the formats I've seen so far.

# Discussion. Effective MongoDB

Let's finish by holding a discussion about 
the strengths and weaknesses of MongoDB,
with a focus on how to counter the weaknesses 
in our application code.