package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import sangria.schema.{Field, ListType, ObjectType}
import models._
import sangria.schema._
import sangria.macros.derive._
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.ast.StringValue

object GraphQLSchema {

  // 1
  /*val LinkType = ObjectType[Unit, Link](
    "Link",
    fields[Unit, Link](
      Field("id", IntType, resolve = _.value.id),
      Field("url", StringType, resolve = _.value.url),
      Field("description", StringType, resolve = _.value.description)
    )
  )*/

  //begining of the object's body:
  implicit val GraphQLDateTime = ScalarType[DateTime](//1
    "DateTime",//2
    coerceOutput = (dt, _) => dt.toString, //3
    coerceInput = { //4
      case StringValue(dt, _, _ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = { //5
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  implicit val LinkType = deriveObjectType[Unit, Link](
    Interfaces(IdentifiableType)
  )
  val UserType = deriveObjectType[Unit, User](Interfaces(IdentifiableType))
  implicit val VoteType = deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType)
  )
  implicit val hasIdLink = HasId[Link, Int](_.id)
  implicit val hasIdUser= HasId[User, Int](_.id)
  implicit val hasIdVote = HasId[Vote, Int](_.id)

  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )
  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )
  val votesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids)
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)

  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.deferOpt(c.arg(Id))
      ),
      Field("links",
        ListType(LinkType),
        arguments = Ids :: Nil,
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      ),
      Field("users",
        ListType(UserType),
        arguments = List(Ids),
        resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),
      Field("vote",
        OptionType(VoteType),
        arguments = Id :: Nil,
        resolve = c => votesFetcher.deferOpt(c.arg(Id))
      ),
      Field("votes",
        ListType(VoteType),
        arguments = Ids :: Nil,
        resolve = c => votesFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}
